package com.zorroa.archivist.web

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.zorroa.archivist.domain.Permission
import com.zorroa.archivist.domain.User
import com.zorroa.archivist.domain.UserProfileUpdate
import com.zorroa.archivist.domain.UserSettings
import com.zorroa.common.util.Json
import com.zorroa.security.Groups
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.stream.Collectors
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@WebAppConfiguration
class UserControllerTests : MockMvcTest() {

    @Test
    @Throws(Exception::class)
    fun testLogin() {
        mvc.perform(
                post("/api/v1/login")
                        .session(admin())
                        .with(SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(status().isOk)
    }

    @Test
    @Throws(Exception::class)
    fun testSendPasswordRecoveryEmail() {
        val user = userService.get("user")
        emailService.sendPasswordResetEmail(user)

        SecurityContextHolder.getContext().authentication = null
        val result = mvc.perform(post("/api/v1/send-password-reset-email")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableMap.of("email", "user@zorroa.com"))))
                .andExpect(status().isOk())
                .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun testSendOnboardEmail() {
        val user =  userService.get("user")

        SecurityContextHolder.getContext().authentication = null
        val result = mvc.perform(post("/api/v1/send-onboard-email")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableMap.of("email", "user@zorroa.com"))))
                .andExpect(status().isOk())
                .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun testResetPassword() {
        val user = userService.get("user")
        val token = emailService.sendPasswordResetEmail(user)
        assertTrue(token.isEmailSent)

        SecurityContextHolder.getContext().authentication = null
        val result = mvc.perform(post("/api/v1/reset-password")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .header("X-Archivist-Recovery-Token", token.token)
                .content(Json.serialize(ImmutableMap.of(
                        "username", "user", "password", "Bilb0Baggins"))))
                .andExpect(status().isOk())
                .andReturn()

        val user1 = Json.deserialize(
                result.response.contentAsByteArray, User::class.java)
        assertEquals(user.id, user1.id)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateProfile() {

        val user1 = userService.get("user")

        val builder = UserProfileUpdate()
        builder.username = "foo"
        builder.email = "test@test.com"
        builder.firstName = "test123"
        builder.lastName = "456test"

        val session = admin()
        val result = mvc.perform(put("/api/v1/users/${user1.id}/_profile")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn()

        val sr = Json.deserialize(
                result.response.contentAsByteArray, object : TypeReference<StatusResult<User>>() {

        })
        val user2 = sr.`object`

        assertEquals(user1.id, user2!!.id)
        assertEquals(builder.username, user2.username)
        assertEquals(builder.email, user2.email)
        assertEquals(builder.firstName, user2.firstName)
        assertEquals(builder.lastName, user2.lastName)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateSettings() {
        val user = userService.get("user")
        val settings = UserSettings()
        settings.search = ImmutableMap.of<String, Any>("foo", "bar")

        val session = admin()
        val result = mvc.perform(put("/api/v1/users/${user.id}/_settings")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(settings))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn()

        val sr = Json.deserialize(
                result.response.contentAsByteArray, object : TypeReference<StatusResult<User>>() {

        })
        val user2 = sr.`object`
        assertEquals(user.id, user2!!.id)
        assertNotNull(settings.search["foo"])
        assertEquals("bar", settings.search["foo"])
    }

    @Test
    @Throws(Exception::class)
    fun testEnableDisable() {
        var user = userService.get("user")
        val session = admin()
        mvc.perform(put("/api/v1/users/" + user.id + "/_enabled")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(ImmutableMap.of("enabled", false)))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn()

        user = userService.get("user")
        assertFalse(user.enabled)

        mvc.perform(put("/api/v1/users/" + user.id + "/_enabled")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(ImmutableMap.of("enabled", true)))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn()

        user = userService.get("user")
        assertTrue(user.enabled)
    }


    @Test
    @Throws(Exception::class)
    fun testDisableSelf() {
        val session = admin()
        mvc.perform(put("/api/v1/users/1/_enabled")
                .content(Json.serialize(ImmutableMap.of("enabled", false)))
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().is4xxClientError())
                .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun testSetPermissions() {

        val user = userService.get("user")
        val perms = permissionService.getPermissions().stream()
                .map { p -> p.id }.collect(Collectors.toList())

        val session = admin()
        val result = mvc.perform(put("/api/v1/users/" + user.id + "/permissions")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(perms))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn()

        val response = Json.Mapper.readValue<List<Permission>>(result.response.contentAsString,
                object : TypeReference<List<Permission>>() {

                })
        assertEquals(response, userService.getPermissions(user))
    }

    @Test
    @Throws(Exception::class)
    fun testGetPermissions() {

        val user = userService.get("user")
        val perms = userService.getPermissions(user) as MutableList
        assertTrue(perms.size > 0)

        userService.setPermissions(user, Lists.newArrayList(permissionService.getPermission(Groups.ADMIN)))
        perms.add(permissionService.getPermission(Groups.ADMIN))

        val session = admin()
        val result = mvc.perform(get("/api/v1/users/" + user.id + "/permissions")
                .session(session)
                .content(Json.serialize(perms))
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk())
                .andReturn()

        val response = Json.Mapper.readValue<List<Permission>>(result.response.contentAsString,
                object : TypeReference<List<Permission>>() {

                })
        assertEquals(response, userService.getPermissions(user))
    }
}

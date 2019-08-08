package com.zorroa.archivist.rest

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.collect.ImmutableMap
import com.google.common.collect.Lists
import com.zorroa.archivist.domain.LocalUserSpec
import com.zorroa.archivist.domain.OrganizationSpec
import com.zorroa.archivist.domain.Permission
import com.zorroa.archivist.domain.User
import com.zorroa.archivist.domain.UserFilter
import com.zorroa.archivist.domain.UserPasswordUpdate
import com.zorroa.archivist.domain.UserProfileUpdate
import com.zorroa.archivist.domain.UserSettings
import com.zorroa.archivist.security.JwtSecurityConstants
import com.zorroa.archivist.security.generateUserToken
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import com.zorroa.security.Groups
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.test.context.web.WebAppConfiguration
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import java.util.stream.Collectors
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@WebAppConfiguration
class UserControllerTests : MockMvcTest() {

    @Test
    fun testLogin() {
        mvc.perform(
            post("/api/v1/login")
                .with(httpBasic("admin", "admin"))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
            .andExpect(status().isOk)
    }

    @Test
    fun testLogoutWithInvalidToken() {
        mvc.perform(
            post("/api/v1/logout?token=abc")
        )
            .andExpect(status().isOk)
    }

    @Test
    fun testApiKey() {
        val session = admin()

        val currentKey = userService.getHmacKey(userService.get("admin"))
        val result = mvc.perform(
            post("/api/v1/users/api-key")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()
        val content = Json.deserialize(result.response.contentAsString, Json.GENERIC_MAP)
        assertEquals(currentKey, content["key"])
    }

    @Test
    fun testApiKeyRegen() {
        val session = admin()
        val currentKey = userService.getHmacKey(userService.get("admin"))

        val spec = UserController.ApiKeyReq(replace = true)
        val result = mvc.perform(
            post("/api/v1/users/api-key")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(spec))
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()
        val content = Json.deserialize(result.response.contentAsString, Json.GENERIC_MAP)
        assertNotEquals(currentKey, content["key"].toString())
    }

    @Test
    fun testSearch() {
        val filter = UserFilter(usernames = listOf("admin"))
        val session = admin()
        val result = mvc.perform(
            post("/api/v1/users/_search")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .headers(admin())
                .content(Json.serialize(filter))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()
        val users = Json.Mapper.readValue<KPagedList<User>>(result.response.contentAsString)
        assertEquals("admin", users[0].username)
    }

    @Test
    fun testFindOne() {
        val filter = UserFilter(usernames = listOf("admin"))
        val session = admin()
        val result = mvc.perform(
            post("/api/v1/users/_findOne")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .headers(admin())
                .content(Json.serialize(filter))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()
        val user = Json.Mapper.readValue<User>(result.response.contentAsString)
        assertEquals("admin", user.username)
    }

    @Test
    fun testCreateV2() {
        val session = admin()

        val spec = LocalUserSpec(
            "bilbo@baggins.com",
            "Bilbo Baggins"
        )

        val result = mvc.perform(
            post("/api/v2/users")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .headers(admin())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()
        val content = Json.deserialize(result.response.contentAsString, Json.GENERIC_MAP)
        assertEquals(spec.email, content["username"])
        assertEquals(spec.email, content["email"])
        assertEquals("Bilbo", content["firstName"])
        assertEquals("Baggins", content["lastName"])
    }

    @Test
    fun testCreateV2WithOrgIdHeader() {
        val user = userService.get("admin")
        val token = generateUserToken(user.id, userService.getHmacKey(user))
        val org = organizationService.create(OrganizationSpec("Mordor Inc"))

        val spec = LocalUserSpec(
            "bilbo@baggins.com",
            "Bilbo Baggins"
        )

        val rsp = mvc.perform(
            post("/api/v2/users")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .header(
                    JwtSecurityConstants.HEADER_STRING_REQ,
                    "${JwtSecurityConstants.TOKEN_PREFIX}$token"
                )
                .header(JwtSecurityConstants.ORGID_HEADER, org.id.toString())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()

        val result = Json.deserialize(rsp.response.contentAsString, Json.GENERIC_MAP)
        assertEquals(
            org.id, UUID.fromString(result["organizationId"] as String),
            "The user's orgId is not the org ID sent with the request header"
        )
    }

    @Test
    @Throws(Exception::class)
    fun testSendPasswordRecoveryEmail() {
        val user = userService.get("user")
        emailService.sendPasswordResetEmail(user)

        val result = mvc.perform(
            post("/api/v1/send-password-reset-email")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableMap.of("email", "user@zorroa.com")))
        )
            .andExpect(status().isOk())
            .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun testSendOnboardEmail() {
        val user = userService.get("user")

        SecurityContextHolder.getContext().authentication = null
        val result = mvc.perform(
            post("/api/v1/send-onboard-email")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(ImmutableMap.of("email", "user@zorroa.com")))
        )
            .andExpect(status().isOk())
            .andReturn()
    }

    @Test
    fun testResetPassword() {
        val user = userService.get("user")
        val token = emailService.sendPasswordResetEmail(user)
        assertTrue(token.isEmailSent)

        val result = mvc.perform(
            post("/api/v1/reset-password")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .header("X-Archivist-Recovery-Token", token.token)
                .content(
                    Json.serialize(
                        ImmutableMap.of(
                            "username", "user", "password", "Bilb0Baggins"
                        )
                    )
                )
        )
            .andExpect(status().isOk())
            .andReturn()

        val user1 = Json.deserialize(
            result.response.contentAsByteArray, User::class.java
        )
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
        val result = mvc.perform(
            put("/api/v1/users/${user1.id}/_profile")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(builder))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
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
    fun testUpdatePassword() {
        val user = userService.get("user")
        val password = UserPasswordUpdate(newPassword = "catandDog!231")

        val session = admin()
        val result = mvc.perform(
            put("/api/v1/users/${user.id}/_password")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(password))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()

        val sr = Json.Mapper.readValue<StatusResult<User>>(result.response.contentAsByteArray)
        assertTrue(sr.success)
        userService.checkPassword("user", password.newPassword)
    }

    @Test
    @Throws(Exception::class)
    fun testUpdateSettings() {
        val user = userService.get("user")
        val settings = UserSettings(search = ImmutableMap.of<String, Any>("foo", "bar"))

        val session = admin()
        val result = mvc.perform(
            put("/api/v1/users/${user.id}/_settings")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(settings))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk())
            .andReturn()

        val sr = Json.deserialize(
            result.response.contentAsByteArray, object : TypeReference<StatusResult<User>>() {
            })
        val user2 = sr.`object`
        assertEquals(user.id, user2!!.id)
        assertNotNull(settings.search!!["foo"])
        assertEquals("bar", settings.search!!["foo"])
    }

    @Test
    @Throws(Exception::class)
    fun testEnableDisable() {
        var user = userService.get("user")
        val session = admin()
        mvc.perform(
            put("/api/v1/users/" + user.id + "/_enabled")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(ImmutableMap.of("enabled", false)))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk())
            .andReturn()

        user = userService.get("user")
        assertFalse(user.enabled)

        mvc.perform(
            put("/api/v1/users/" + user.id + "/_enabled")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(ImmutableMap.of("enabled", true)))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk())
            .andReturn()

        user = userService.get("user")
        assertTrue(user.enabled)
    }

    @Test
    @Throws(Exception::class)
    fun testDisableSelf() {
        mvc.perform(
            put("/api/v1/users/1/_enabled")
                .content(Json.serialize(ImmutableMap.of("enabled", false)))
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().is4xxClientError())
            .andReturn()
    }

    @Test
    @Throws(Exception::class)
    fun testDelete() {
        val user = userService.get("user")
        mvc.perform(
            delete("/api/v1/users/${user.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andReturn()

        assertFalse(userService.exists("user", "local"))
    }

    @Test
    @Throws(Exception::class)
    fun testSetPermissions() {

        val user = userService.get("user")
        val perms = permissionService.getPermissions().stream()
            .map { p -> p.id }.collect(Collectors.toList())

        val session = admin()
        val result = mvc.perform(
            put("/api/v1/users/" + user.id + "/permissions")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .content(Json.serialize(perms))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
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
        val result = mvc.perform(
            get("/api/v1/users/" + user.id + "/permissions")
                .headers(admin())
                .content(Json.serialize(perms))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk())
            .andReturn()

        val response = Json.Mapper.readValue<List<Permission>>(result.response.contentAsString,
            object : TypeReference<List<Permission>>() {
            })
        assertEquals(response, userService.getPermissions(user))
    }
}

package com.zorroa.archivist.security

import com.auth0.jwt.JWT
import com.zorroa.archivist.AbstractTest
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokenStoreTests : AbstractTest() {

    @Autowired
    lateinit var tokenStore: TokenStore

    @Test
    fun testcreateSessionToken() {
        val user = userService.get("admin")
        val token = tokenStore.createSessionToken(user.id)

        val jwt = JWT.decode(token)
        // Must have a token EXP date.
        assertNotNull(jwt.expiresAt)
        // Must have a session Id.
        assertNotNull(jwt.claims["sessionId"])
        // Must have a userId
        assertEquals(user.id, UUID.fromString(jwt.claims["userId"]?.asString()))
    }

    @Test
    fun removeSessionToken() {
        val user = userService.get("admin")
        val token = tokenStore.createSessionToken(user.id)
        val jwt = JWT.decode(token)
        assertTrue(tokenStore.removeSession(jwt.claims["sessionId"]!!.asString()))
        assertFalse(tokenStore.removeSession(jwt.claims["sessionId"]!!.asString()))
    }

    @Test
    fun testCreateAPIToken() {
        val user = userService.get("admin")
        val token = tokenStore.createAPIToken(user.id)

        val jwt = JWT.decode(token)
        // No exp date for API tokens.
        assertNull(jwt.expiresAt)
        // No sessions for API tokens.
        assertNull(jwt.claims["sessionId"])
        // Must have a userId.
        assertEquals(user.id, UUID.fromString(jwt.claims["userId"]?.asString()))
    }

    @Test
    fun testBase64UUID() {
        assertEquals(22, base64UUID(UUID.randomUUID()).length)
    }
}
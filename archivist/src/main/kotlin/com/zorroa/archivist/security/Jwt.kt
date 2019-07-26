package com.zorroa.archivist.security

import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.archivist.security.JwtSecurityConstants.HEADER_STRING
import com.zorroa.archivist.security.JwtSecurityConstants.ORGID_HEADER
import com.zorroa.archivist.security.JwtSecurityConstants.TOKEN_PREFIX
import com.zorroa.archivist.service.UserService
import com.zorroa.archivist.service.event
import com.zorroa.security.Groups
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import java.io.IOException
import java.util.UUID
import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JWTAuthorizationFilter(authManager: AuthenticationManager) :
    BasicAuthenticationFilter(authManager) {

    @Autowired
    private lateinit var validator: MasterJwtValidator

    @Throws(IOException::class, ServletException::class)
    override fun doFilterInternal(
        req: HttpServletRequest,
        res: HttpServletResponse,
        chain: FilterChain
    ) {

        val token = req.getHeader(HEADER_STRING)
        if (token == null || !token.startsWith(TOKEN_PREFIX)) {
            chain.doFilter(req, res)
            return
        }
        /**
         * Validate the JWT claims. Assuming they are valid, create a JwtAuthenticationToken which
         * will be converted into a proper UsernamePasswordAuthenticationToken by the
         * JwtAuthenticationProvider
         *
         * Not doing this 2 step process means the actuator endpoints can't be authed by a token.
         */
        val validated = validator.validate(token.replace(TOKEN_PREFIX, ""))

        val authToken = JwtAuthenticationToken(validated.claims, req.getHeader(ORGID_HEADER))
        SecurityContextHolder.getContext().authentication = authToken

        req.setAttribute("authType", HttpServletRequest.CLIENT_CERT_AUTH)
        res.addHeader(HEADER_STRING,  token)
        chain.doFilter(req, res)
    }
}

/**
 * JwtAuthenticationToken stores the validated claims
 *
 * @param claims The map JWT claims
 * @property organizationId An organizationId override value. Super-admins only.
 *
 */
class JwtAuthenticationToken constructor(
    claims: Map<String, String>,
    val organizationId: String? = null
) : AbstractAuthenticationToken(listOf()) {

    val userId = claims.getValue("userId")
    val sessionId = claims.getOrDefault("sessionId", "")

    override fun getCredentials(): Any {
        return sessionId
    }

    override fun getPrincipal(): Any {
        return userId
    }

    override fun isAuthenticated(): Boolean = false
}

/**
 * An AuthenticationProvider that specifically looks for JwtAuthenticationToken.  Pulls
 * the userId from the JwtAuthenticationToken and returns a UsernamePasswordAuthenticationToken.
 */
class JwtAuthenticationProvider : AuthenticationProvider {

    @Autowired
    private lateinit var userService: UserService

    override fun authenticate(auth: Authentication): Authentication {
        val token = auth as JwtAuthenticationToken
        val userId = token.userId

        // Grab the user record out to verify the user is actually provisioned
        val user = userService.get(UUID.fromString(userId))

        // Pull the user's permissions
        val authorities = userService.getPermissions(user).toSet()

        // Build a new UserAuthed object
        val authedUser = UserAuthed(
            user.id,
            user.organizationId,
            user.username,
            authorities,
            user.attrs
        )

        // If the token has an orgId validate
        if (token.organizationId != null) {
            if (authedUser.authorities.map { it.authority == Groups.SUPERADMIN }.isNotEmpty()) {
                authedUser.organizationId = UUID.fromString(token.organizationId)
                logger.event(
                    LogObject.USER, LogAction.ORGSWAP,
                    mapOf(
                        "username" to user.username,
                        "overrideOrganizationId" to token.organizationId
                    )
                )
            }
        }

        return UsernamePasswordAuthenticationToken(authedUser, token.credentials, authorities)
    }

    override fun supports(cls: Class<*>): Boolean {
        return cls == JwtAuthenticationToken::class.java
    }

    companion object {

        private val logger = LoggerFactory.getLogger(JwtAuthenticationProvider::class.java)
    }
}

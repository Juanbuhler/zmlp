package com.zorroa.archivist.rest

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Sets
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.*
import com.zorroa.archivist.service.EmailService
import com.zorroa.archivist.service.PermissionService
import com.zorroa.archivist.service.UserService
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.security.Groups
import io.micrometer.core.annotation.Timed
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.security.Principal
import java.util.*
import java.util.stream.Collectors
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


@RestController
@Timed
class UserController @Autowired constructor(
        private val userService: UserService,
        private val permissionService: PermissionService,
        private val emailService: EmailService
) {

    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).MANAGER) || hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @RequestMapping(value = ["/api/v1/users"])
    fun getAll() : List<User> = userService.getAll()

    @RequestMapping(value = ["/api/v1/who"])
    fun getCurrent(user: Principal?): ResponseEntity<Any> {
        return if (user != null) {
            ResponseEntity(userService.get(getUserId()), HttpStatus.OK)
        }
        else {
            ResponseEntity(mapOf("message" to "No authenticated user"), HttpStatus.UNAUTHORIZED)
        }
    }

    class ApiKeyReq(
            val replace: Boolean = false,
            val server: String? = null
    )

    @RequestMapping(value = ["/api/v1/users/api-key"], method=[RequestMethod.GET, RequestMethod.POST])
    fun getApiKey(@RequestBody(required = false) kreq: ApiKeyReq?, hreq: HttpServletRequest): Any {
        val req = kreq ?: ApiKeyReq(false, null)

        /**
         * Select where the URI in the key is going to come from.
         */
        val uri = when {
            req.server != null -> req.server
            hreq.getHeader( "X-Zorroa-Curator-Host") != null -> hreq.getHeader( "X-Zorroa-Curator-Protocol") + "://" + hreq.getHeader( "X-Zorroa-Curator-Host")
            else -> {
                val builder = ServletUriComponentsBuilder.fromCurrentRequestUri()
                builder.replacePath("/").build().toString()
            }
        }

        val user = getUser()
        val spec = ApiKeySpec(user.id, user.username, req.replace, uri)
        return userService.getApiKey(spec)
    }


    /**
     * An HTTP auth based login endpoint.
     *
     * Returns the current user as well as a X-Zorroa-Auth-Token header with a
     * valid JWT token.
     *
     * @return
     */
    @PostMapping(value = ["/api/v1/login"])
    fun login(): ResponseEntity<User> {
        val user = getUser()
        val headers = HttpHeaders()
        headers.add("X-Zorroa-Auth-Token",
                generateUserToken(user.id, userService.getHmacKey(user)))

        return ResponseEntity.ok()
                .headers(headers)
                .body(userService.get(user.id))
    }

    /**
     * An HTTP auth based logout endpoint.
     *
     * @return
     */
    @RequestMapping(value = ["/api/v1/logout"], method=[RequestMethod.POST, RequestMethod.GET])
    fun logout(req: HttpServletRequest, rsp: HttpServletResponse) : Any {
        val auth = getAuthentication()
        val cookieClearingLogoutHandler = CookieClearingLogoutHandler(AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY)
        val securityContextLogoutHandler = SecurityContextLogoutHandler()
        cookieClearingLogoutHandler.logout(req, rsp, auth)
        securityContextLogoutHandler.logout(req, rsp, auth)
        SecurityContextHolder.clearContext()

        return if (auth == null) {
            mapOf("success" to false)
        }
        else {
            mapOf("success" to true)
        }
    }

    /**
     * This handles a password reset using the reset token and the
     * ResetPasswordSecurityFilter.
     * @return
     * @throws ServletException
     */
    @PostMapping(value = ["/api/v1/reset-password"])
    @Throws(ServletException::class)
    fun resetPasswordAndLogin(): User {
        return userService.get(getUserId())
    }

    class SendForgotPasswordEmailRequest {
        var email: String? = null
    }

    @PostMapping(value = ["/api/v1/send-password-reset-email"])
    @Throws(ServletException::class)
    fun sendPasswordRecoveryEmail(@RequestBody req: SendForgotPasswordEmailRequest): Any {
        val user = userService.get(req.email!!)
        emailService.sendPasswordResetEmail(user)
        return HttpUtils.status("send-password-reset-email", "update", true)
    }

    @PostMapping(value = ["/api/v1/send-onboard-email"])
    @Throws(ServletException::class)
    fun sendOnboardEmail(@RequestBody req: SendForgotPasswordEmailRequest): Any {
        val user = userService.get(req.email!!)
        emailService.sendOnboardEmail(user)
        return HttpUtils.status("send-onboard-email", "update", true)
    }

    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).MANAGER) || hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PostMapping(value = ["/api/v1/users"])
    fun create(@RequestBody builder: UserSpec): User {
        return userService.create(builder)
    }

    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).MANAGER) || hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PostMapping(value = ["/api/v2/users"])
    fun createV2(@RequestBody spec: LocalUserSpec): User {
        return userService.create(spec)
    }

    @RequestMapping(value = ["/api/v1/users/{id}"])
    operator fun get(@PathVariable id: UUID): User {
        validatePermissions(id)
        return userService.get(id)
    }

    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).MANAGER) || hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @RequestMapping(value = ["/api/v1/users/{username}/_exists"])
    operator fun get(@PathVariable username: String): Map<*, *> {
        return ImmutableMap.of("result", userService.exists(username, null))
    }

    @PutMapping(value = ["/api/v1/users/{id}/_profile"])
    fun updateProfile(@RequestBody form: UserProfileUpdate, @PathVariable id: UUID): Any {
        validatePermissions(id)
        val user = userService.get(id)
        return HttpUtils.updated("users", id, userService.update(user, form), userService.get(id))
    }

    @PutMapping(value = ["/api/v1/users/{id}/_password"])
    fun updatePassword(@RequestBody form: UserPasswordUpdate, @PathVariable id: UUID): Any {
        validatePermissions(id)

        /**
         * If the Ids match, then the user is the current user, so validate the existing password.
         */
        if (id == getUserId()) {
            val storedPassword = userService.getPassword(getUsername())
            if (!BCrypt.checkpw(form.oldPassword, storedPassword)) {
                throw IllegalArgumentException("Existing password invalid")
            }
        }

        val user = userService.get(id)
        userService.resetPassword(user, form.newPassword)

        return HttpUtils.updated("users", id, true, user)
    }

    @PutMapping(value = ["/api/v1/users/{id}/_settings"])
    fun updateSettings(@RequestBody settings: UserSettings, @PathVariable id: UUID): Any {
        validatePermissions(id)
        val user = userService.get(id)
        return HttpUtils.updated("users", id, userService.updateSettings(user, settings), userService.get(id))
    }

    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).MANAGER) || hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PutMapping(value = ["/api/v1/users/{id}/_enabled"])
    fun disable(@RequestBody settings: Map<String, Boolean>, @PathVariable id: UUID): Any {
        val user = userService.get(id)
        if (id == getUserId()) {
            throw IllegalArgumentException("You cannot disable yourself")
        }

        if (settings["enabled"] == null) {
            throw IllegalArgumentException("missing 'enabled' value, must be true or false")
        }

        return HttpUtils.status("users", id, "enable",
                userService.setEnabled(user, settings.getValue("enabled")))
    }

    /**
     * Return the list of permissions for the given user id.
     *
     * @param id
     * @return
     */
    @GetMapping(value = ["/api/v1/users/{id}/permissions"])
    fun getPermissions(@PathVariable id: UUID): List<Permission> {
        validatePermissions(id)
        val user = userService.get(id)
        return userService.getPermissions(user)
    }

    /**
     * Set an array of integers that correspond to permission IDs.  These
     * will be assigned to the user as permissions.  The Permission object
     * assigned are returned back.
     *
     * @param pids
     * @param id
     * @return
     */
    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).MANAGER) || hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PutMapping(value = ["/api/v1/users/{id}/permissions"])
    fun setPermissions(@RequestBody pids: List<UUID>, @PathVariable id: UUID): List<Permission> {
        val user = userService.get(id)
        val perms = pids.stream().map { i -> permissionService.getPermission(i) }.collect(Collectors.toList())
        userService.setPermissions(user, perms)
        return userService.getPermissions(user)
    }

    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).MANAGER) || hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PutMapping(value = ["/api/v1/users/{id}/permissions/_add"])
    fun addPermissions(@RequestBody pids: List<String>, @PathVariable id: UUID): List<Permission> {
        val user = userService.get(id)
        val resolved = Sets.newHashSetWithExpectedSize<Permission>(pids.size)
        pids.mapTo(resolved) { permissionService.getPermission(it) }
        userService.addPermissions(user, resolved)
        return userService.getPermissions(user)
    }

    @PreAuthorize("hasAuthority(T(com.zorroa.security.Groups).MANAGER) || hasAuthority(T(com.zorroa.security.Groups).ADMIN)")
    @PutMapping(value = ["/api/v1/users/{id}/permissions/_remove"])
    fun removePermissions(@RequestBody pids: List<String>, @PathVariable id: UUID): List<Permission> {
        val user = userService.get(id)
        val resolved = Sets.newHashSetWithExpectedSize<Permission>(pids.size)
        pids.mapTo(resolved) { permissionService.getPermission(it) }
        userService.removePermissions(user, resolved)
        return userService.getPermissions(user)
    }

    private fun validatePermissions(id: UUID) {
        if (!Objects.equals(getUserId(),id) && !hasPermission(Groups.MANAGER, Groups.ADMIN)) {
            throw SecurityException("Access denied.")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserController::class.java)
    }
}

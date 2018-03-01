package com.zorroa.archivist.web.api

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Sets
import com.zorroa.archivist.HttpUtils
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.SecurityUtils
import com.zorroa.archivist.service.EmailService
import com.zorroa.archivist.service.PermissionService
import com.zorroa.archivist.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler
import org.springframework.validation.BindingResult
import org.springframework.web.bind.annotation.*
import java.util.stream.Collectors
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.validation.Valid

@RestController
class UserController @Autowired constructor(
        private val userService: UserService,
        private val permissionService: PermissionService,
        private val emailService: EmailService
) {

    @RequestMapping(value = ["/api/v1/api-key"])
    fun getApiKey(): String {
        return try {
            userService.getHmacKey(SecurityUtils.getUsername())
        } catch (e: Exception) {
            ""
        }
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
    @RequestMapping(value = ["/api/v1/users"])
    fun getAll() : List<User> = userService.getAll()

    @RequestMapping(value = ["/api/v1/who"])
    fun getCurrent(): ResponseEntity<Any> {
        return if (SecurityUtils.getUserOrNull() != null) {
            ResponseEntity(userService.get(SecurityUtils.getUser().id), HttpStatus.OK)
        }
        else {
            ResponseEntity(HttpStatus.UNAUTHORIZED)
        }
    }

    @Deprecated("")
    @PostMapping(value = ["/api/v1/generate_api_key"])
    fun generate_api_key_V1(): String {
        return userService.generateHmacKey(SecurityUtils.getUsername())
    }

    @PostMapping(value = ["/api/v1/api-key"])
    fun generate_api_key(): String {
        return userService.generateHmacKey(SecurityUtils.getUsername())
    }

    /**
     * An HTTP auth based login endpoint.
     *
     * @return
     */
    @PostMapping(value = ["/api/v1/login"])
    fun login(): User {
        return userService.get(SecurityUtils.getUser().id)
    }

    /**
     * An HTTP auth based logout endpoint.
     *
     * @return
     */
    @RequestMapping(value = ["/api/v1/logout"], method=[RequestMethod.POST, RequestMethod.GET])
    fun logout(req: HttpServletRequest, rsp: HttpServletResponse) : Any {
        val auth = SecurityUtils.getAuthentication()
        if (auth == null) {
            return mapOf("success" to false)
        }

        return if (auth is AnonymousAuthenticationToken) {
            mapOf("success" to false)
        }
        else {
            SecurityContextLogoutHandler().logout(req, rsp, auth)
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
        return userService.get(SecurityUtils.getUser().id)
    }

    class SendForgotPasswordEmailRequest {
        var email: String? = null
    }

    @PostMapping(value = ["/api/v1/send-password-reset-email"])
    @Throws(ServletException::class)
    fun sendPasswordRecoveryEmail(@RequestBody req: SendForgotPasswordEmailRequest): Any {
        val user = userService.getByEmail(req.email!!)
        emailService.sendPasswordResetEmail(user)
        return HttpUtils.status("send-password-reset-email", "update", true)
    }

    @PostMapping(value = ["/api/v1/send-onboard-email"])
    @Throws(ServletException::class)
    fun sendOnboardEmail(@RequestBody req: SendForgotPasswordEmailRequest): Any {
        val user = userService.getByEmail(req.email!!)
        emailService.sendOnboardEmail(user)
        return HttpUtils.status("send-onboard-email", "update", true)
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
    @PostMapping(value = ["/api/v1/users"])
    fun create(@Valid @RequestBody builder: UserSpec, bindingResult: BindingResult): User {
        if (bindingResult.hasErrors()) {
            throw RuntimeException("Failed to add user")
        }
        return userService.create(builder)
    }

    @RequestMapping(value = ["/api/v1/users/{id}"])
    operator fun get(@PathVariable id: Int): User {
        validatePermissions(id)
        return userService.get(id)
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
    @RequestMapping(value = ["/api/v1/users/{username}/_exists"])
    operator fun get(@PathVariable username: String): Map<*, *> {
        return ImmutableMap.of("result", userService.exists(username))
    }

    @PutMapping(value = ["/api/v1/users/{id}/_profile"])
    fun updateProfile(@RequestBody form: UserProfileUpdate, @PathVariable id: Int): Any {
        validatePermissions(id)
        val user = userService.get(id)
        return HttpUtils.updated("users", id, userService.update(user, form), userService.get(id))
    }

    @PutMapping(value = ["/api/v1/users/{id}/_password"])
    fun updatePassword(@RequestBody form: UserPasswordUpdate, @PathVariable id: Int): Any {
        validatePermissions(id)

        /**
         * If the Ids match, then the user is the current user, so validate the existing password.
         */
        if (id == SecurityUtils.getUser().id) {
            val storedPassword = userService.getPassword(SecurityUtils.getUsername())
            if (!BCrypt.checkpw(form.oldPassword, storedPassword)) {
                throw IllegalArgumentException("Existing password invalid")
            }
        }

        val user = userService.get(id)
        userService.resetPassword(user, form.newPassword)

        return HttpUtils.updated("users", id, true, user)
    }

    @PutMapping(value = ["/api/v1/users/{id}/_settings"])
    fun updateSettings(@RequestBody settings: UserSettings, @PathVariable id: Int): Any {
        validatePermissions(id)
        val user = userService.get(id)
        return HttpUtils.updated("users", id, userService.updateSettings(user, settings), userService.get(id))
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
    @PutMapping(value = ["/api/v1/users/{id}/_enabled"])
    fun disable(@RequestBody settings: Map<String, Boolean>, @PathVariable id: Int): Any {
        val user = userService.get(id)
        if (id == SecurityUtils.getUser().id) {
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
    fun getPermissions(@PathVariable id: Int): List<Permission> {
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
    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
    @PutMapping(value = ["/api/v1/users/{id}/permissions"])
    fun setPermissions(@RequestBody pids: List<Int>, @PathVariable id: Int): List<Permission> {
        val user = userService.get(id)
        val perms = pids.stream().map { i -> permissionService.getPermission(i) }.collect(Collectors.toList())
        userService.setPermissions(user, perms)
        return userService.getPermissions(user)
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
    @PutMapping(value = ["/api/v1/users/{id}/permissions/_add"])
    fun addPermissions(@RequestBody pids: List<String>, @PathVariable id: Int): List<Permission> {
        val user = userService.get(id)
        val resolved = Sets.newHashSetWithExpectedSize<Permission>(pids.size)
        pids.mapTo(resolved) { permissionService.getPermission(it) }
        userService.addPermissions(user, resolved)
        return userService.getPermissions(user)
    }

    @PreAuthorize("hasAuthority('group::manager') || hasAuthority('group::administrator')")
    @PutMapping(value = ["/api/v1/users/{id}/permissions/_remove"])
    fun removePermissions(@RequestBody pids: List<String>, @PathVariable id: Int): List<Permission> {
        val user = userService.get(id)
        val resolved = Sets.newHashSetWithExpectedSize<Permission>(pids.size)
        pids.mapTo(resolved) { permissionService.getPermission(it) }
        userService.removePermissions(user, resolved)
        return userService.getPermissions(user)
    }

    private fun validatePermissions(id: Int) {
        if (SecurityUtils.getUser().id != id && !SecurityUtils.hasPermission("group::manager")) {
            throw SecurityException("Access denied.")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserController::class.java)
    }
}

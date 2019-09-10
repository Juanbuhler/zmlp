package com.zorroa.archivist.security

import com.google.common.collect.Sets
import com.google.common.collect.Sets.intersection
import com.zorroa.archivist.domain.Access
import com.zorroa.archivist.domain.Acl
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.Permission
import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.common.domain.ArchivistWriteException
import com.zorroa.common.util.Json
import com.zorroa.security.Groups
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCrypt
import java.util.UUID

/**
 * Set a new Authentication value and return the previous one, or null in the case
 * that no Authentication exists.
 *
 * @param auth The new Authentication value.
 * @return the old authentication value or null
 */
fun resetAuthentication(auth: Authentication?): Authentication? {
    val oldAuth = SecurityContextHolder.getContext().authentication
    SecurityContextHolder.getContext().authentication = auth
    return oldAuth
}

/**
 * Execute the given code using the provided Authentication object.  If Authentication
 * is null, then just execute with no authentication.
 *
 */
fun <T> withAuth(auth: Authentication?, body: () -> T): T {
    val hold = SecurityContextHolder.getContext().authentication
    SecurityContextHolder.getContext().authentication = auth
    try {
        return body()
    } finally {
        SecurityContextHolder.getContext().authentication = hold
    }
}

object SecurityLogger {
    val logger = LoggerFactory.getLogger(SecurityLogger::class.java)
}

fun getAuthentication(): Authentication? {
    return SecurityContextHolder.getContext().authentication
}

fun getSecurityContext(): SecurityContext {
    return SecurityContextHolder.getContext()
}

fun createPasswordHash(plainPassword: String): String {
    return BCrypt.hashpw(plainPassword, BCrypt.gensalt())
}

/**
 * Generate a alpha-numeric random password of the given length.
 *
 * @param length The password length
 * @return A random password.
 */
fun generateRandomPassword(length: Int): String {
    val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    return (1..length).map { allowedChars.random() }.joinToString("")
}

fun getUser(): UserAuthed {
    val auth = SecurityContextHolder.getContext().authentication
    return if (auth == null) {
        throw AuthenticationCredentialsNotFoundException("No login credentials specified")
    } else {
        try {
            auth.principal as UserAuthed
        } catch (e1: ClassCastException) {
            try {
                auth.details as UserAuthed
            } catch (e2: ClassCastException) {
                // Log this message so we can see what the type is.
                SecurityLogger.logger.warn(
                    "Invalid auth objects: principal='{}' details='{}'",
                    auth?.principal, auth?.details
                )
                throw AuthenticationCredentialsNotFoundException("Invalid auth object, UserAuthed object not found")
            }
        }
    }
}

fun getAnalystEndpoint(): String {
    val auth = SecurityContextHolder.getContext().authentication
    return if (auth == null) {
        throw AuthenticationCredentialsNotFoundException("No login credentials specified for cluster node")
    } else {
        return SecurityContextHolder.getContext().authentication.principal as String
    }
}

fun getUserOrNull(): UserAuthed? {
    return try {
        getUser()
    } catch (ex: AuthenticationCredentialsNotFoundException) {
        null
    }
}

fun getUsername(): String {
    return getUser().username
}

fun getUserId(): UUID {
    return getUser().id
}

fun getOrgId(): UUID {
    return getUser().organizationId
}

fun hasPermission(permIds: Set<UUID>?): Boolean {
    if (permIds.orEmpty().isEmpty()) {
        return true
    }
    return if (hasPermission(Groups.ADMIN)) {
        true
    } else intersection<UUID>(permIds, getPermissionIds()).isNotEmpty()
}

fun hasPermission(vararg perms: String, adminOverride: Boolean = true): Boolean {
    return hasPermission(perms.toSet(), adminOverride)
}

private fun containsOnlySuperadmin(perms: Collection<String>): Boolean {
    return perms.isNotEmpty() and perms.all { it == Groups.SUPERADMIN }
}

private fun containsSuperadmin(it: Collection<GrantedAuthority>) =
    it.any { it.authority == Groups.SUPERADMIN }

fun hasPermission(perms: Collection<String>, adminOverride: Boolean = true): Boolean {
    val auth = SecurityContextHolder.getContext().authentication
    auth?.authorities?.let { authorities ->
        if (containsSuperadmin(authorities)) {
            return true
        } else if (!containsOnlySuperadmin(perms)) {
            for (g in authorities) {
                if ((adminOverride && g.authority == Groups.ADMIN) || perms.contains(g.authority)) {
                    return true
                }
            }
        }
    }
    return false
}

/**
 *
 */
fun hasPermission(access: Access, asset: Document): Boolean {
    val user = getUser()
    // This assumes that the filter was already applied.
    return if (user.hasPermissionFilter()) {
        true
    } else {
        val perms = asset.getAttr("system.permissions.${access.field}", Json.SET_OF_UUIDS)
        return hasPermission(perms)
    }
}

/**
 * Test that the current logged in user has the given access
 * with a particular access control list.  Users with group::superuser
 * will always have access.
 *
 * @param acl
 * @param access
 * @return
 */
fun hasPermission(acl: Acl?, access: Access): Boolean {
    if (acl == null) {
        return true
    }
    return if (hasPermission(Groups.ADMIN)) {
        true
    } else acl.hasAccess(getPermissionIds(), access)
}

fun getPermissionIds(): Set<UUID> {
    val result = Sets.newHashSet<UUID>()

    for (g in SecurityContextHolder.getContext().authentication.authorities) {
        try {
            val p = g as Permission
            result.add(p.id)
        } catch (e: ClassCastException) {
        }
    }
    return result
}

fun getOrganizationFilter(): QueryBuilder {
    return QueryBuilders.termQuery("system.organizationId", getOrgId().toString())
}

/**
 * Return true if the user can set the new ACL.
 *
 * This function checks to ensure that A user isn't taking away access they have by accident.
 *
 * @param newAcl
 * @param oldAcl
 */
fun canSetAclOnFolder(newAcl: Acl?, oldAcl: Acl?, created: Boolean) {
    if (newAcl == null || oldAcl == null) {
        throw IllegalArgumentException("Cannot determine new folder ACL, neither new or old can be null")
    }

    if (hasPermission(Groups.ADMIN)) {
        return
    }

    if (created) {
        if (!hasPermission(newAcl, Access.Read)) {
            throw ArchivistWriteException("You cannot create a folder without read access to it.")
        }
    } else {
        /**
         * Here we check to to see if you have read/write/export access already
         * and we don't let you take away access from yourself.
         */
        val hasRead = hasPermission(oldAcl, Access.Read)
        val hasWrite = hasPermission(oldAcl, Access.Write)
        val hasExport = hasPermission(oldAcl, Access.Export)

        if (hasRead && !hasPermission(newAcl, Access.Read)) {
            throw ArchivistWriteException("You cannot remove read access from yourself.")
        }

        if (hasWrite && !hasPermission(newAcl, Access.Write)) {
            throw ArchivistWriteException("You cannot remove write access from yourself.")
        }

        if (hasExport && !hasPermission(newAcl, Access.Export)) {
            throw ArchivistWriteException("You cannot remove export access from yourself.")
        }
    }
}

/**
 * Return true if the current user can export an asset.
 *
 * @param asset
 * @return
 */
fun canExport(asset: Document): Boolean {
    if (hasPermission(Groups.EXPORT, Groups.ADMIN)) {
        return true
    }

    val perms = asset.getAttr("system.permissions.export", Json.SET_OF_UUIDS)
    return hasPermission(perms)
}

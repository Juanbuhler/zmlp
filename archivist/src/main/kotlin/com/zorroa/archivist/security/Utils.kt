package com.zorroa.archivist.security

import com.google.common.collect.Sets
import com.google.common.collect.Sets.intersection
import com.zorroa.archivist.domain.Acl
import com.zorroa.archivist.domain.Permission
import com.zorroa.archivist.sdk.security.UserAuthed
import com.zorroa.common.domain.Access
import com.zorroa.common.domain.ArchivistWriteException
import com.zorroa.common.domain.Document
import com.zorroa.common.schema.PermissionSchema
import com.zorroa.common.util.Json
import com.zorroa.security.Groups
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCrypt
import java.util.*

fun getAuthentication(): Authentication? {
    return SecurityContextHolder.getContext().authentication
}

fun createPasswordHash(plainPassword: String): String {
    return BCrypt.hashpw(plainPassword, BCrypt.gensalt())
}

fun getUser(): UserAuthed {
    return if (SecurityContextHolder.getContext().authentication == null) {
        throw AuthenticationCredentialsNotFoundException("No login credentials specified")
    } else {
        try {
            SecurityContextHolder.getContext().authentication.principal as UserAuthed
        } catch (e1: ClassCastException) {
            try {
                SecurityContextHolder.getContext().authentication.details as UserAuthed
            } catch (e2: ClassCastException) {
                throw AuthenticationCredentialsNotFoundException("Invalid login creds, UserAuthed object not found")
            }

        }

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
    if (permIds == null) {
        return true
    }
    if (permIds == null || permIds.isEmpty()) {
        return true
    }
    return if (hasPermission(Groups.ADMIN)) {
        true
    } else !intersection<UUID>(permIds, getPermissionIds()).isEmpty()
}

fun hasPermission(vararg perms: String): Boolean {
    return hasPermission(perms.toSet())
}

fun hasPermission(perms: Collection<String>): Boolean {
    val auth = SecurityContextHolder.getContext().authentication

    auth?.authorities?.let{
        for (g in it) {
            if (Objects.equals(g.authority, Groups.ADMIN) || perms.contains(g.authority)) {
                return true
            }
        }
    }
    return false
}

/**
 * Return true if the user has permission to a particular type of permission.
 *
 * @param field
 * @param asset
 * @return
 */
fun hasPermission(field: String, asset: Document): Boolean {
    val perms = asset.getAttr("zorroa.permissions.$field", Json.SET_OF_UUIDS)
    return hasPermission(perms)
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
        val p = g as Permission
        result.add(p.id)
    }
    return result
}

fun getPermissionsFilter(access: Access?): QueryBuilder? {
    if (hasPermission(Groups.ADMIN)) {
        return null
    } else {
        if (access == null || access == Access.Read) {
            return if (hasPermission(Groups.READ)) {
                null
            } else {
                QueryBuilders.termsQuery("zorroa.permissions.read", getPermissionIds())
            }
        }
        else if (access == Access.Write) {
            return if (hasPermission(Groups.WRITE)) {
                null
            } else {
                QueryBuilders.termsQuery("zorroa.permissions.write", getPermissionIds())
            }
        }
        else if (access == Access.Export) {
            return if (hasPermission(Groups.EXPORT)) {
                null
            } else {
                QueryBuilders.termsQuery("zorroa.permissions.export", getPermissionIds())
            }
        }
    }

    return QueryBuilders.termsQuery("zorroa.permissions.read", getPermissionIds())
}

fun setWritePermissions(source: Document, perms: Collection<Permission>) {
    var ps: PermissionSchema? = source.getAttr("zorroa.permissions", PermissionSchema::class.java)
    if (ps == null) {
        ps = PermissionSchema()
    }
    ps.write.clear()
    for (p in perms) {
        ps.write.add(p.id)
    }
    source.setAttr("zorroa.permissions", ps)
}

fun setExportPermissions(source: Document, perms: Collection<Permission>) {
    var ps: PermissionSchema? = source.getAttr("zorroa.permissions", PermissionSchema::class.java)
    if (ps == null) {
        ps = PermissionSchema()
    }
    ps.export.clear()
    for (p in perms) {
        ps.export.add(p.id)
    }
    source.setAttr("zorroa.permissions", ps)
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

    val perms = asset.getAttr("zorroa.permissions.export", Json.SET_OF_UUIDS)
    return hasPermission(perms)
}


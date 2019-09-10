package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Organization
import com.zorroa.archivist.domain.Permission
import com.zorroa.archivist.domain.PermissionFilter
import com.zorroa.archivist.domain.PermissionSpec
import com.zorroa.archivist.repository.PermissionDao
import com.zorroa.archivist.util.StaticUtils.UUID_REGEXP
import com.zorroa.common.repository.KPagedList
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.stream.Collectors

interface PermissionService {

    fun findPermission(filter: PermissionFilter): Permission

    fun createStandardPermissions(org: Organization)

    fun getPermissions(): List<Permission>

    fun getPermissions(filter: PermissionFilter): KPagedList<Permission>

    fun getPermission(id: UUID): Permission

    fun createPermission(builder: PermissionSpec): Permission

    fun getPermissionNames(): List<String>

    fun permissionExists(authority: String): Boolean

    fun getPermission(name: String): Permission

    fun deletePermission(permission: Permission): Boolean
}

@Service
@Transactional
class PermissionServiceImpl @Autowired constructor(
    private val permissionDao: PermissionDao
) : PermissionService {

    @Transactional(readOnly = true)
    override fun findPermission(filter: PermissionFilter): Permission {
        return permissionDao.findOne(filter)
    }

    override fun getPermissions(): List<Permission> {
        return permissionDao.getAll()
    }

    override fun getPermissions(filter: PermissionFilter): KPagedList<Permission> {
        return permissionDao.getAll(filter)
    }

    override fun getPermissionNames(): List<String> {
        return permissionDao.getAll().stream().map { p -> p.fullName }.collect(Collectors.toList<String>())
    }

    override fun permissionExists(authority: String): Boolean {
        return permissionDao.exists(authority)
    }

    override fun getPermission(name: String): Permission {
        return if (UUID_REGEXP.matches(name)) {
            permissionDao.get(UUID.fromString(name))
        } else {
            permissionDao.get(name)
        }
    }

    override fun getPermission(id: UUID): Permission {
        return permissionDao.get(id)
    }

    override fun createPermission(builder: PermissionSpec): Permission {
        require(builder.name != "superadmin") { "Cannot create permission " +
        "'${builder.type}::${builder.name}' in the Zorroa namespace." }
        return permissionDao.create(builder, false)
    }

    val standardPerms = listOf(
            mapOf("type" to "zorroa", "name" to "administrator", "desc" to "Superuser, can do and access everything"),
            mapOf("type" to "zorroa", "name" to "everyone", "desc" to "A standard user of the system"),
            mapOf("type" to "assets", "name" to "export-all", "desc" to "Export all files"),
            mapOf("type" to "assets", "name" to "view-all", "desc" to "Read all data"),
            mapOf("type" to "assets", "name" to "edit-all", "desc" to "Write all data"),
            mapOf("type" to "assets", "name" to "delete-all", "desc" to "Write all data"),
            mapOf("type" to "zorroa", "name" to "librarian", "desc" to "Manager /library folder"),
            mapOf("type" to "folders", "name" to "view-all", "desc" to "Ability to view all folders"))

    override fun createStandardPermissions(org: Organization) {
        for (entry in standardPerms) {
            val spec = PermissionSpec(entry.getValue("type"), entry.getValue("name"))
            permissionDao.create(spec, true)
        }
    }

    override fun deletePermission(permission: Permission): Boolean {
        return permissionDao.delete(permission)
    }
}

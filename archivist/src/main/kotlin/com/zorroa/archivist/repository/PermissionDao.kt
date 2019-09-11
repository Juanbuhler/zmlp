package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.Acl
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.Permission
import com.zorroa.archivist.domain.PermissionFilter
import com.zorroa.archivist.domain.PermissionSpec
import com.zorroa.archivist.domain.PermissionUpdateSpec
import com.zorroa.archivist.domain.User
import com.zorroa.archivist.sdk.security.UserId
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.service.event
import com.zorroa.archivist.util.JdbcUtils
import com.zorroa.archivist.util.StaticUtils.UUID_REGEXP
import com.zorroa.common.repository.KPage
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.schema.PermissionSchema
import com.zorroa.security.Groups
import org.springframework.dao.DuplicateKeyException
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

interface PermissionDao {

    fun getAll(): List<Permission>

    fun create(builder: PermissionSpec, immutable: Boolean): Permission

    fun update(permission: PermissionUpdateSpec): Permission

    fun renameUserPermission(user: User, newName: String): Boolean

    fun get(id: UUID): Permission

    fun getId(name: String): UUID

    fun get(authority: String): Permission

    fun findOne(filter: PermissionFilter): Permission

    fun resolveAcl(acl: Acl?, createMissing: Boolean): Acl

    fun getAll(filter: PermissionFilter): KPagedList<Permission>

    fun count(): Long

    fun count(filter: PermissionFilter): Long

    fun exists(name: String): Boolean

    fun getAll(user: UserId): List<Permission>

    fun getAll(type: String): List<Permission>

    fun get(type: String, name: String): Permission

    fun getAll(ids: Collection<UUID>?): List<Permission>

    fun getAll(names: List<String>?): List<Permission>

    /**
     * Delete the given permission.  Optionally force deleting a system permission
     * that users would not otherwise be abl to delete.
     */
    fun delete(perm: Permission, force: Boolean = false): Boolean

    fun getDefaultPermissionSchema(): PermissionSchema
}

@Repository
class PermissionDaoImpl : AbstractDao(), PermissionDao {

    override fun create(spec: PermissionSpec, immutable: Boolean): Permission {

        val id = uuid1.generate()
        try {
            jdbc.update { connection ->
                val ps = connection.prepareStatement(INSERT)
                ps.setObject(1, id)
                ps.setObject(2, getOrgId())
                ps.setString(3, spec.name)
                ps.setString(4, spec.type)
                ps.setString(5, spec.type + "::" + spec.name)
                ps.setString(
                    6, if (spec.description == null)
                        String.format("%s permission", spec.name)
                    else
                        spec.description
                )
                ps.setString(7, spec.source)
                ps.setBoolean(8, immutable)
                ps
            }
            logger.event(
                LogObject.PERMISSION, LogAction.CREATE,
                mapOf("permissionId" to id, "permissionName" to spec.name)
            )
        } catch (e: DuplicateKeyException) {
            throw DuplicateKeyException("The permission " + spec.name + " already exists")
        }

        return get(id)
    }

    override fun update(permission: PermissionUpdateSpec): Permission {
        val authority = arrayOf(permission.type, permission.name).joinToString(Permission.JOIN)
        jdbc.update(
            "UPDATE permission SET str_type=?, str_name=?,str_description=?,str_authority=? WHERE pk_organization=? AND pk_permission=? AND bool_immutable=?",
            permission.type, permission.name, permission.description, authority, getOrgId(), permission.id, false
        )
        return get(permission.id)
    }

    override fun renameUserPermission(user: User, newName: String): Boolean {
        return jdbc.update(
            "UPDATE permission SET str_name=?, str_authority=? WHERE pk_organization=? AND pk_permission=?",
            newName, "user::$newName", getOrgId(), user.permissionId
        ) == 1
    }

    override operator fun get(id: UUID): Permission {
        val orgId = getOrgId()
        return throwWhenNotFound("Permission '$id' was not found for '$orgId'") {
            jdbc.queryForObject(
                "SELECT * FROM permission WHERE pk_organization=? AND pk_permission=?",
                MAPPER, orgId, id
            )
        }
    }

    override fun findOne(filter: PermissionFilter): Permission {
        filter.apply { page = KPage(0, 1) }
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("Permission not found") {
            return KPagedList(1L, filter.page, jdbc.query(query, MAPPER, *values))[0]
        }
    }

    override fun getId(name: String): UUID {
        if (UUID_REGEXP.matches(name)) {
            return UUID.fromString(name)
        }
        val orgId = getOrgId()
        return throwWhenNotFound("Permission '$name' was not found for org: $orgId") {
            return jdbc.queryForObject(
                "SELECT pk_permission FROM permission WHERE pk_organization=? AND str_authority=?",
                UUID::class.java, orgId, name
            )
        }
    }

    override fun get(type: String, name: String): Permission {
        val orgId = getOrgId()
        return throwWhenNotFound("Permission '$type::$name' was not found for org: '$orgId'") {
            jdbc.queryForObject(
                "SELECT * FROM permission WHERE pk_organization=? AND str_name=? AND str_type=?",
                MAPPER, orgId, name, type
            )
        }
    }

    override operator fun get(authority: String): Permission {
        val parts = authority.split(Permission.JOIN.toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        return get(parts[0], parts[1])
    }

    override fun resolveAcl(acl: Acl?, createMissing: Boolean): Acl {
        if (acl == null) {
            return Acl()
        }

        val resolved = mutableSetOf<UUID>()

        val result = Acl()
        for (entry in acl) {

            if (entry.getPermissionId() == null) {
                // Get the permission ID
                val id = try {
                    getId(entry.permission)
                } catch (e: EmptyResultDataAccessException) {
                    if (createMissing) {
                        create(
                            PermissionSpec(entry.permission)
                                .apply { description = "Auto created permission" }, false
                        ).id
                    } else {
                        throw e
                    }
                }
                if (!resolved.contains(id)) {
                    result.addEntry(id, entry.getAccess())
                    resolved.add(id)
                }
            } else {
                if (!resolved.contains(entry.permissionId)) {
                    result.add(entry)
                    resolved.add(entry.permissionId)
                }
            }
        }
        return result
    }

    override fun getAll(): List<Permission> {
        return jdbc.query("SELECT * FROM permission WHERE pk_organization=? ", MAPPER, getOrgId())
    }

    override fun getAll(filter: PermissionFilter): KPagedList<Permission> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    override fun count(): Long {
        return jdbc.queryForObject("$COUNT WHERE pk_organization=?", Long::class.java, getOrgId())
    }

    override fun count(filter: PermissionFilter): Long {
        return jdbc.queryForObject(
            filter.getQuery(COUNT, forCount = true),
            Long::class.java, *filter.getValues(forCount = true)
        )
    }

    override fun exists(name: String): Boolean {
        return jdbc.queryForObject(
            "$COUNT WHERE pk_organization=? AND str_authority=?",
            Int::class.java, getOrgId(), name
        ) == 1
    }

    override fun getAll(user: UserId): List<Permission> {
        return jdbc.query(GET_BY_USER, MAPPER, user.id)
    }

    override fun getAll(type: String): List<Permission> {
        return jdbc.query(
            "SELECT * FROM permission WHERE pk_organization=? AND str_type=?",
            MAPPER, getOrgId(), type
        )
    }

    override fun getAll(ids: Collection<UUID>?): List<Permission> {
        return if (ids == null || ids.isEmpty()) {
            listOf()
        } else {
            val values = ids.toTypedArray().plus(getOrgId())
            jdbc.query(
                "SELECT * FROM permission WHERE " +
                    JdbcUtils.`in`("pk_permission", ids.size) + " AND pk_organization=?", MAPPER,
                *values
            )
        }
    }

    override fun getAll(names: List<String>?): List<Permission> {
        return if (names == null || names.isEmpty()) {
            listOf()
        } else {
            val values: MutableList<Any> = mutableListOf()
            names.toCollection(values)
            values.add(getOrgId())
            jdbc.query(
                "SELECT * FROM permission WHERE " + JdbcUtils.`in`("str_authority", names.size) +
                    "AND pk_organization=?", MAPPER, *values.toTypedArray()
            )
        }
    }

    override fun delete(perm: Permission, force: Boolean): Boolean {
        return if (force) {
            jdbc.update(
                "DELETE FROM permission WHERE pk_organization=? AND pk_permission=?",
                getOrgId(), perm.id
            )
        } else {
            jdbc.update(
                "DELETE FROM permission WHERE pk_organization=? AND pk_permission=? AND bool_immutable=?",
                getOrgId(), perm.id, force
            )
        } == 1
    }

    /**
     * TODO: hard coded permissions
     * This is just hard coded for now, post MVP organizations will be able to customizes this.
     */
    override fun getDefaultPermissionSchema(): PermissionSchema {
        val everyone = get(Groups.EVERYONE).id
        val admin: UUID = get(Groups.ADMIN).id
        val schema = PermissionSchema()
        schema.addToRead(everyone)
        schema.addToExport(everyone)
        schema.addToWrite(admin)
        schema.addToDelete(admin)
        return schema
    }

    companion object {

        private val INSERT = JdbcUtils.insert(
            "permission",
            "pk_permission",
            "pk_organization",
            "str_name",
            "str_type",
            "str_authority",
            "str_description",
            "str_source",
            "bool_immutable"
        )

        private val MAPPER = RowMapper { rs, _ ->
            Permission(
                rs.getObject("pk_permission") as UUID,
                rs.getString("str_name"),
                rs.getString("str_type"),
                rs.getString("str_description"),
                rs.getBoolean("bool_immutable")
            )
        }

        private const val COUNT = "SELECT COUNT(1) FROM permission"

        private const val GET = "SELECT * FROM permission "

        private const val GET_BY_USER = "SELECT p.* " +
            "FROM " +
            "permission p, " +
            "user_permission m " +
            "WHERE " +
            "p.pk_permission=m.pk_permission " +
            "AND " +
            "m.pk_user=?"
    }
}

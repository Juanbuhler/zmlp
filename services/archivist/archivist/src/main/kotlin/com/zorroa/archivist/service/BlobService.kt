package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Access
import com.zorroa.archivist.domain.Acl
import com.zorroa.archivist.domain.Blob
import com.zorroa.archivist.domain.BlobId
import com.zorroa.archivist.domain.BlobSpec
import com.zorroa.archivist.domain.SetPermissions
import com.zorroa.archivist.repository.BlobDao
import com.zorroa.archivist.repository.PermissionDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface BlobService {

    fun set(app: String, feature: String, name: String, blob: Any): Blob

    fun set(app: String, feature: String, name: String, spec: BlobSpec): Blob

    fun get(app: String, feature: String, name: String): Blob

    fun delete(blob: BlobId): Boolean

    fun getId(app: String, feature: String, name: String, forAccess: Access): BlobId

    fun getPermissions(blob: BlobId): Acl

    fun setPermissions(blob: BlobId, perms: SetPermissions): Acl

    fun getAll(app: String, feature: String): List<Blob>
}

@Service
@Transactional
class BlobServiceImpl
    @Autowired constructor (
        private val blobDao: BlobDao,
        private val permissionDao: PermissionDao
    ) : BlobService {

    override fun set(app: String, feature: String, name: String, blob: Any): Blob {
        return try {
            val id = blobDao.getId(app, feature, name, Access.Write)
            blobDao.update(id, blob)
            blobDao[app, feature, name]
        } catch (e: EmptyResultDataAccessException) {
            blobDao.create(app, feature, name, blob)
        }
    }

    override fun set(app: String, feature: String, name: String, spec: BlobSpec): Blob {
        var created = false
        val blob = try {
            val id = blobDao.getId(app, feature, name, Access.Write)
            blobDao.update(id, spec.data)
            blobDao[app, feature, name]
        } catch (e: EmptyResultDataAccessException) {
            created = true
            blobDao.create(app, feature, name, spec.data)
        }

        if (created) {
            if (spec.acl != null) {
                permissionDao.resolveAcl(spec.acl, false)
                blobDao.setPermissions(blob, SetPermissions(spec.acl, true))
            }
        }
        return blob
    }

    override fun get(app: String, feature: String, name: String): Blob {
        return blobDao[app, feature, name]
    }

    override fun getId(app: String, feature: String, name: String, forAccess: Access): BlobId {
        return blobDao.getId(app, feature, name, forAccess)
    }

    override fun delete(blob: BlobId): Boolean {
        return blobDao.delete(blob)
    }

    override fun getPermissions(blob: BlobId): Acl {
        return blobDao.getPermissions(blob)
    }

    override fun setPermissions(blob: BlobId, perms: SetPermissions): Acl {
        permissionDao.resolveAcl(perms.acl, false)
        return blobDao.setPermissions(blob, perms)
    }

    override fun getAll(app: String, feature: String): List<Blob> {
        return blobDao.getAll(app, feature)
    }
}

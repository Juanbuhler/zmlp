package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Credentials
import com.zorroa.archivist.domain.CredentialsSpec
import com.zorroa.archivist.domain.CredentialsUpdate
import com.zorroa.archivist.repository.CredentialsCustomDao
import com.zorroa.archivist.repository.CredentialsDao
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import com.zorroa.zmlp.service.security.EncryptionService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

interface CredentialsService {
    fun create(spec: CredentialsSpec): Credentials
    fun get(id: UUID): Credentials
    fun delete(id: UUID)
    fun update(id: UUID, update: CredentialsUpdate): Credentials
    fun getDecryptedBlob(id: UUID): String
    fun setEncryptedBlob(id: UUID, clearText: String)
}

@Service
@Transactional
class CredentialsServiceImpl(
    val credentialsDao: CredentialsDao,
    val credentialsCustomDao: CredentialsCustomDao,
    val encryptionService: EncryptionService
) : CredentialsService {

    @PersistenceContext
    lateinit var entityManager: EntityManager

    override fun create(spec: CredentialsSpec): Credentials {
        val id = UUIDGen.uuid1.generate()
        val time = System.currentTimeMillis()
        val actor = getZmlpActor()

        val creds = Credentials(
            id,
            getProjectId(),
            spec.name,
            spec.type,
            time,
            time,
            actor.toString(),
            actor.toString()
        )

        logger.event(LogObject.CREDENTIALS, LogAction.CREATE, mapOf("newCredentialsId" to id))
        val created = credentialsDao.saveAndFlush(creds)
        setEncryptedBlob(id, spec.blob)
        return created
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): Credentials {
        return credentialsDao.getOneByProjectIdAndId(getProjectId(), id)
    }

    override fun delete(id: UUID) {
        logger.event(LogObject.CREDENTIALS, LogAction.DELETE, mapOf("credentialsId" to id))
        credentialsDao.delete(get(id))
    }

    override fun update(id: UUID, update: CredentialsUpdate): Credentials {
        val current = get(id)
        entityManager.detach(current)

        update.blob?.let {
            val cryptedText = encryptionService.encryptString(it, Credentials.CRYPT_VARIANCE)
            credentialsCustomDao.updateEncryptedBlob(id, cryptedText)
        }

        logger.event(LogObject.CREDENTIALS, LogAction.UPDATE, mapOf("credentialsId" to id))
        val creds = credentialsDao.save(current.getUpdated(update))
        return get(creds.id)
    }

    override fun setEncryptedBlob(id: UUID, clearText: String) {
        val cryptedText = encryptionService.encryptString(clearText, Credentials.CRYPT_VARIANCE)
        credentialsCustomDao.updateEncryptedBlob(id, cryptedText)
    }

    @Transactional(readOnly = true)
    override fun getDecryptedBlob(id: UUID): String {
        logger.event(LogObject.CREDENTIALS, LogAction.DECRYPT, mapOf("credentialsId" to id))
        return encryptionService.decryptString(
            credentialsCustomDao.getEncryptedBlob(id), Credentials.CRYPT_VARIANCE)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CredentialsServiceImpl::class.java)
    }
}

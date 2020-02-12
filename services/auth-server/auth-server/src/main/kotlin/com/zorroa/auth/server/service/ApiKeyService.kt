package com.zorroa.auth.server.service

import com.zorroa.auth.server.domain.ApiKey
import com.zorroa.auth.server.domain.ApiKeyFilter
import com.zorroa.auth.server.domain.ApiKeySpec
import com.zorroa.auth.server.repository.ApiKeyCustomRepository
import com.zorroa.auth.server.repository.ApiKeyRepository
import com.zorroa.auth.server.repository.PagedList
import com.zorroa.auth.server.security.KeyGenerator
import com.zorroa.auth.server.security.getProjectId
import com.zorroa.auth.server.security.getZmlpActor
import com.zorroa.zmlp.apikey.Permission
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import com.zorroa.zmlp.service.security.EncryptionService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface ApiKeyService {

    /**
     * Create a new API key.
     */
    fun create(spec: ApiKeySpec): ApiKey

    fun get(id: UUID): ApiKey

    fun findAll(): List<ApiKey>

    fun findOne(filter: ApiKeyFilter): ApiKey

    fun search(filter: ApiKeyFilter): PagedList<ApiKey>

    fun delete(apiKey: ApiKey)
}

@Service
@Transactional
class ApiKeyServiceImpl constructor(
    val apikeyCustomRepository: ApiKeyCustomRepository,
    val apiKeyRepository: ApiKeyRepository

) : ApiKeyService {

    @Autowired
    lateinit var encryptionService: EncryptionService

    override fun create(spec: ApiKeySpec): ApiKey {
        if (!getZmlpActor().hasAnyPermission(Permission.SystemServiceKey)) {
            validatePermissionsCanBeAssigned(spec.permissions)
        }

        val time = System.currentTimeMillis()
        val actor = getZmlpActor()
        val key = ApiKey(
            UUID.randomUUID(),
            getProjectId(),
            KeyGenerator.generate(24),
            encryptionService.encryptString(KeyGenerator.generate(64), ApiKey.CRYPT_VARIANCE),
            spec.name,
            spec.permissions.map { it.name }.toSet(),
            time, time,
            actor.toString(),
            actor.toString()
        )

        logger.event(
            LogObject.API_KEY, LogAction.CREATE,
            mapOf(
                "apiKeyId" to key.id,
                "apiKeyName" to key.name
            )
        )

        try {
            return apiKeyRepository.saveAndFlush(key)
        } catch (ex: DataIntegrityViolationException) {
            throw(DataIntegrityViolationException("Data Integrity Violation: Verify your Api Key"))
        }
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): ApiKey {
        return apiKeyRepository.findByProjectIdAndId(getProjectId(), id)
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<ApiKey> {
        return apiKeyRepository.findAllByProjectId(getProjectId())
    }

    @Transactional(readOnly = true)
    override fun findOne(filter: ApiKeyFilter): ApiKey {
        return apikeyCustomRepository.findOne(filter)
    }

    @Transactional(readOnly = true)
    override fun search(filter: ApiKeyFilter): PagedList<ApiKey> {
        return apikeyCustomRepository.search(filter)
    }

    override fun delete(apiKey: ApiKey) {
        logger.event(
            LogObject.API_KEY, LogAction.DELETE,
            mapOf(
                "apiKeyId" to apiKey.id,
                "apiKeyName" to apiKey.name
            )
        )
        apiKeyRepository.delete(apiKey)
    }

    fun validatePermissionsCanBeAssigned(perms: Set<Permission>) {
        perms.forEach {
            if (it.internal) {
                throw IllegalArgumentException("Permission ${it.name} does not exist")
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApiKeyServiceImpl::class.java)
    }
}

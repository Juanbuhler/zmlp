package com.zorroa.archivist.service

import com.zorroa.archivist.domain.DataSource
import com.zorroa.archivist.domain.DataSourceSpec
import com.zorroa.archivist.domain.DataSourceUpdate
import com.zorroa.archivist.domain.FileType

import com.zorroa.archivist.domain.JobFilter
import com.zorroa.archivist.domain.JobState
import com.zorroa.archivist.repository.DataSourceDao
import com.zorroa.archivist.repository.DataSourceJdbcDao
import com.zorroa.archivist.repository.UUIDGen
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.security.getZmlpActor
import com.zorroa.archivist.util.isUUID
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

interface DataSourceService {

    /**
     * Create a new [DataSource]
     */
    fun create(spec: DataSourceSpec): DataSource

    /**
     * Update an existing [DataSource]
     */
    fun update(id: UUID, update: DataSourceUpdate): DataSource

    /**
     * Delete an existing [DataSource]
     */
    fun delete(id: UUID)

    /**
     * Get a [DataSource] by its unique ID.
     */
    fun get(id: UUID): DataSource

    /**
     * Set available credentials blobs for this job.
     */
    fun setCredentials(id: UUID, names: Set<String>)
}

@Service
@Transactional
class DataSourceServiceImpl(
    val dataSourceDao: DataSourceDao,
    val dataSourceJdbcDao: DataSourceJdbcDao,
    val credentialsService: CredentialsService,
    val txEvent: TransactionEventManager
) : DataSourceService {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var pipelineService: PipelineService

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @PersistenceContext
    lateinit var entityManager: EntityManager

    override fun create(spec: DataSourceSpec): DataSource {

        val time = System.currentTimeMillis()
        val actor = getZmlpActor()
        val id = UUIDGen.uuid1.generate()

        val mods = pipelineModService.getByNames(spec.modules ?: setOf())

        val result = dataSourceDao.saveAndFlush(
            DataSource(
                id,
                getProjectId(),
                spec.name,
                spec.uri,
                spec.fileTypes,
                listOf(),
                mods.map { it.id },
                time,
                time,
                actor.name,
                actor.name
            )
        )

        val creds = credentialsService.getAll(spec.credentials)
        dataSourceJdbcDao.setCredentials(id, creds)

        logger.event(
            LogObject.DATASOURCE, LogAction.CREATE,
            mapOf(
                "newDataSourceId" to result.id,
                "newDataSourceName" to result.name
            )
        )
        entityManager.detach(result)
        return get(result.id)
    }

    override fun update(id: UUID, update: DataSourceUpdate): DataSource {
        logger.event(LogObject.DATASOURCE, LogAction.UPDATE, mapOf("dataSourceId" to id))

        val mods = pipelineModService.getByNames(update.modules ?: listOf())
        val ds = get(id)

        val updated = DataSource(
            ds.id,
            ds.projectId,
            update.name,
            update.uri,
            FileType.fromArray(update.fileTypes),
            listOf(),
            mods.map { it.id },
            ds.timeCreated,
            System.currentTimeMillis(),
            ds.actorCreated,
            getZmlpActor().toString()
        )

        dataSourceDao.saveAndFlush(updated)
        update.credentials?.let {
            setCredentials(id, it)
        }

        entityManager.detach(ds)
        return get(id)
    }

    override fun delete(id: UUID) {
        logger.event(LogObject.DATASOURCE, LogAction.DELETE, mapOf("dataSourceId" to id))
        val ds = get(id)

        // Grab all the jobs before the DS is deleted.
        val jobs = jobService.getAll(
            JobFilter(
                states = listOf(JobState.InProgress, JobState.Failure),
                datasourceIds = listOf(ds.id)
            ).apply { page.disabled = true }
        )

        logger.info("Canceling ${jobs.size()} DataSource ${ds.id} jobs")

        // If this thing commits we async cancel all the jobs for the
        // DS which involves killing all the running tasks.
        txEvent.afterCommit(sync = false) {
            jobs.forEach { jobService.cancelJob(it) }
        }
        dataSourceDao.delete(ds)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): DataSource {
        return dataSourceDao.getOneByProjectIdAndId(getProjectId(), id)
    }

    override fun setCredentials(id: UUID, names: Set<String>) {
        val creds = names.map {
            if (it.isUUID()) {
                credentialsService.get(UUID.fromString(it))
            } else {
                credentialsService.get(it)
            }
        }
        dataSourceJdbcDao.setCredentials(id, creds)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DataSourceServiceImpl::class.java)
    }
}

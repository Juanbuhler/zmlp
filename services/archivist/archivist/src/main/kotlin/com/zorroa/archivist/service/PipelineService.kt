package com.zorroa.archivist.service

import com.zorroa.archivist.domain.Pipeline
import com.zorroa.archivist.domain.PipelineFilter
import com.zorroa.archivist.domain.PipelineMode
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.PipelineUpdate
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.repository.PipelineDao
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

interface PipelineService {
    fun create(spec: PipelineSpec): Pipeline
    fun get(id: UUID): Pipeline
    fun get(name: String): Pipeline
    fun getAll(filter: PipelineFilter): KPagedList<Pipeline>
    fun update(id: UUID, update: PipelineUpdate): Boolean
    fun delete(id: UUID): Boolean
    fun findOne(filter: PipelineFilter): Pipeline
}

/**
 * The PipelineService handles the management of pipelines.
 */
@Service
@Transactional
class PipelineServiceImpl @Autowired constructor(
    val pipelineDao: PipelineDao
) : PipelineService {

    @Autowired
    lateinit var pipelineModService: PipelineModService

    override fun create(spec: PipelineSpec): Pipeline {
        if (spec.mode == PipelineMode.CUSTOM) {
            spec.modules = null
        } else {
            spec.processors = null
        }
        val pipeline = pipelineDao.create(spec)

        logger.event(
            LogObject.PIPELINE, LogAction.CREATE,
            mapOf(
                "pipelineId" to pipeline.id,
                "pipelineName" to pipeline.name
            )
        )

        spec.modules?.let {
            pipelineDao.setPipelineMods(pipeline.id, pipelineModService.getByNames(it))
        }
        return get(pipeline.id)
    }

    override fun update(id: UUID, update: PipelineUpdate): Boolean {
        val pipeline = get(id)
        if (pipeline.mode == PipelineMode.CUSTOM) {
            update.modules = listOf()
        } else {
            update.processors = listOf()
        }

        logger.event(
            LogObject.PIPELINE, LogAction.UPDATE,
            mapOf(
                "pipelineId" to pipeline.id,
                "pipelineName" to pipeline.name
            )
        )

        return pipelineDao.update(pipeline.id, update)
    }

    @Transactional(readOnly = true)
    override fun get(id: UUID): Pipeline = pipelineDao.get(id)

    @Transactional(readOnly = true)
    override fun get(name: String): Pipeline = pipelineDao.get(name)

    @Transactional(readOnly = true)
    override fun getAll(filter: PipelineFilter): KPagedList<Pipeline> = pipelineDao.getAll(filter)

    @Transactional(readOnly = true)
    override fun findOne(filter: PipelineFilter): Pipeline = pipelineDao.findOne(filter)

    override fun delete(id: UUID): Boolean{

        logger.event(
            LogObject.PIPELINE, LogAction.DELETE,
            mapOf(
                "pipelineId" to id
            )
        )

        return pipelineDao.delete(id)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineServiceImpl::class.java)
    }
}

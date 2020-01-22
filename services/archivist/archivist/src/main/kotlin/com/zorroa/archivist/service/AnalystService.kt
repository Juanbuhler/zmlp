package com.zorroa.archivist.service

import com.zorroa.archivist.clients.RestClient
import com.zorroa.archivist.domain.Analyst
import com.zorroa.archivist.domain.AnalystFilter
import com.zorroa.archivist.domain.AnalystSpec
import com.zorroa.archivist.domain.AnalystState
import com.zorroa.archivist.domain.LockState
import com.zorroa.archivist.domain.TaskState
import com.zorroa.archivist.repository.AnalystDao
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.security.getAnalyst
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.event
import com.zorroa.zmlp.service.logging.warnEvent
import com.zorroa.zmlp.util.Json
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.util.UUID

interface AnalystService {
    fun upsert(spec: AnalystSpec): Analyst
    fun exists(endpoint: String): Boolean
    fun getAll(filter: AnalystFilter): KPagedList<Analyst>
    fun get(id: UUID): Analyst
    fun setLockState(analyst: Analyst, state: LockState): Boolean
    fun isLocked(endpoint: String): Boolean
    fun getUnresponsive(state: AnalystState, duration: Duration): List<Analyst>
    fun delete(analyst: Analyst): Boolean
    fun setState(analyst: Analyst, state: AnalystState): Boolean
    fun getClient(endpoint: String): RestClient
    fun killTask(endpoint: String, taskId: UUID, reason: String, newState: TaskState): Boolean
    fun setTaskId(analyst: Analyst, taskId: UUID?): Boolean
    fun findOne(filter: AnalystFilter): Analyst
}

@Service
@Transactional
class AnalystServicImpl @Autowired constructor(
    val analystDao: AnalystDao,
    val taskDao: TaskDao
) : AnalystService {

    override fun upsert(spec: AnalystSpec): Analyst {
        val analyst = getAnalyst()
        spec.taskId?.let {
            taskDao.updatePingTime(it, analyst.endpoint)
        }

        return if (analystDao.update(spec)) {
            analystDao.get(analyst.endpoint)
        } else {
            val analyst = analystDao.create(spec)
            logger.info("Created analyst: {}", analyst.endpoint)
            analyst
        }
    }

    override fun exists(endpoint: String): Boolean {
        return analystDao.exists(endpoint)
    }

    override fun findOne(filter: AnalystFilter): Analyst {
        return analystDao.findOne(filter)
    }

    override fun get(id: UUID): Analyst {
        return analystDao.get(id)
    }

    @Transactional(readOnly = true)
    override fun getAll(filter: AnalystFilter): KPagedList<Analyst> {
        return analystDao.getAll(filter)
    }

    override fun setLockState(analyst: Analyst, state: LockState): Boolean {
        return analystDao.setLockState(analyst, state)
    }

    @Transactional(readOnly = true)
    override fun isLocked(endpoint: String): Boolean {
        return analystDao.isInLockState(endpoint, LockState.Locked)
    }

    override fun setTaskId(analyst: Analyst, taskId: UUID?): Boolean {
        return analystDao.setTaskId(analyst.endpoint, taskId)
    }

    override fun setState(analyst: Analyst, state: AnalystState): Boolean {
        return analystDao.setState(analyst, state)
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    override fun killTask(endpoint: String, taskId: UUID, reason: String, newState: TaskState): Boolean {
        return try {
            val client = RestClient(endpoint)
            val result = client.delete(
                "/kill/$taskId",
                mapOf("reason" to reason, "newState" to newState.name), Json.GENERIC_MAP
            )

            return if (result["status"] as Boolean) {
                logger.event(LogObject.TASK, LogAction.KILL, mapOf("reason" to reason, "taskId" to taskId))
                true
            } else {
                logger.warnEvent(
                    LogObject.TASK, LogAction.KILL, "Failed to kill task",
                    mapOf("taskId" to taskId, "analyst" to endpoint)
                )
                false
            }
        } catch (e: Exception) {
            logger.warnEvent(
                LogObject.TASK, LogAction.KILL, "Failed to kill task",
                mapOf("taskId" to taskId, "analyst" to endpoint), e
            )
            false
        }
    }

    @Transactional(readOnly = true)
    override fun getUnresponsive(state: AnalystState, duration: Duration): List<Analyst> {
        return analystDao.getUnresponsive(state, duration)
    }

    override fun delete(analyst: Analyst): Boolean {
        return analystDao.delete(analyst)
    }

    override fun getClient(endpoint: String): RestClient {
        return RestClient(endpoint)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AnalystServicImpl::class.java)
    }
}

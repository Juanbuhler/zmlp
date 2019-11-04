package com.zorroa.archivist.repository

import com.google.common.base.Preconditions
import com.zorroa.archivist.domain.AssetCounters
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.service.MeterRegistryHolder.getTags
import com.zorroa.archivist.service.event
import com.zorroa.common.domain.InternalTask
import com.zorroa.common.domain.JobId
import com.zorroa.common.domain.JobState
import com.zorroa.common.domain.Task
import com.zorroa.common.domain.TaskFilter
import com.zorroa.common.domain.TaskId
import com.zorroa.common.domain.TaskSpec
import com.zorroa.common.domain.TaskState
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import com.zorroa.common.util.Json
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.time.Duration
import java.util.UUID

interface TaskDao {
    fun create(job: JobId, spec: TaskSpec): Task
    fun get(id: UUID): Task
    fun getInternal(id: UUID): InternalTask
    fun setState(task: TaskId, newState: TaskState, oldState: TaskState?): Boolean
    fun setHostEndpoint(task: TaskId, host: String)
    fun getHostEndpoint(taskId: TaskId): String?
    fun setExitStatus(task: TaskId, exitStatus: Int)
    fun getScript(id: UUID): ZpsScript
    fun incrementAssetCounters(task: TaskId, counts: AssetCounters): Boolean
    fun getAll(tf: TaskFilter?): KPagedList<Task>
    fun getAll(job: UUID, state: TaskState): List<InternalTask>
    fun isAutoRetryable(task: TaskId): Boolean

    /**
     * Return the total number of pending tasks.
     */
    fun getPendingTaskCount(): Long

    /**
     * Update the task's ping time as long as it's running on the given endpoint.
     */
    fun updatePingTime(taskId: UUID, endpoint: String): Boolean

    /**
     * Return a list of [InternalTask]s which have not seen a ping for the given [Duration]
     */
    fun getOrphans(duration: Duration): List<InternalTask>

    /**
     * Reset the asset stat counters for the given task back to 0.
     */
    fun resetAssetCounters(task: TaskId): Boolean

    fun findOne(filter: TaskFilter): Task
}

@Repository
class TaskDaoImpl : AbstractDao(), TaskDao {

    @Value("\${archivist.dispatcher.autoRetryLimit}")
    lateinit var autoRetryLimit: Number

    override fun create(job: JobId, spec: TaskSpec): Task {
        Preconditions.checkNotNull(spec.name)

        val id = uuid1.generate()
        val time = System.currentTimeMillis()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, job.jobId)
            ps.setString(3, spec.name.trim())
            ps.setLong(4, time)
            ps.setLong(5, time)
            ps.setLong(6, time)
            ps.setLong(7, time)
            ps.setString(8, Json.serializeToString(spec.script, "{}"))
            ps.setInt(9, 0)
            ps
        }

        val totalAssets = spec.script?.over?.size ?: 0
        jdbc.update("INSERT INTO task_stat (pk_task, pk_job,int_asset_total_count) VALUES (?,?,?)",
                id, job.jobId, totalAssets)
        logger.event(LogObject.TASK, LogAction.CREATE,
                mapOf("taskId" to id, "taskName" to spec.name, "assetCount" to totalAssets))
        return get(id)
    }

    override fun get(id: UUID): Task {
        return jdbc.queryForObject("$GET WHERE task.pk_task=?", MAPPER, id)
    }

    override fun getInternal(id: UUID): InternalTask {
        return jdbc.queryForObject("$GET_INTERNAL WHERE task.pk_task=?", INTERNAL_MAPPER, id)
    }

    override fun getScript(id: UUID): ZpsScript {
        val script = jdbc.queryForObject("SELECT json_script FROM task WHERE pk_task=?",
                String::class.java, id)
        return Json.deserialize(script, ZpsScript::class.java)
    }

    override fun setState(task: TaskId, newState: TaskState, oldState: TaskState?): Boolean {
        val time = System.currentTimeMillis()
        // Note: There is a trigger updating counts here.
        val updated = if (oldState != null) {
            jdbc.update("UPDATE task SET int_state=?,time_modified=? WHERE pk_task=? AND int_state=?",
                    newState.ordinal, time, task.taskId, oldState.ordinal) == 1
        } else {
            jdbc.update("UPDATE task SET int_state=?,time_modified=? WHERE pk_task=?",
                    newState.ordinal, time, task.taskId) == 1
        }

        if (updated) {
            meterRegistry.counter("zorroa.task.state", getTags(newState.metricsTag())).increment()
            logger.event(LogObject.TASK, LogAction.STATE_CHANGE,
                    mapOf("taskId" to task.taskId,
                            "newState" to newState.name,
                            "oldState" to oldState?.name))

            if (newState in START_STATES) {
                jdbc.update("UPDATE task SET time_started=?, int_run_count=int_run_count+1, " +
                        "time_stopped=-1 WHERE pk_task=?", time, task.taskId)
            } else if (newState in STOP_STATES) {
                jdbc.update("UPDATE task SET time_stopped=? WHERE pk_task=?", time, task.taskId)
            }
        }

        return updated
    }

    override fun setHostEndpoint(task: TaskId, host: String) {
        jdbc.update("UPDATE task SET str_host=? WHERE pk_task=?", host, task.taskId)
    }

    override fun getHostEndpoint(taskId: TaskId): String? {
        return try {
            jdbc.queryForObject("SELECT str_host FROM task WHERE pk_task=?",
                    String::class.java, taskId.taskId)
        } catch (e: EmptyResultDataAccessException) {
            null
        }
    }

    override fun setExitStatus(task: TaskId, exitStatus: Int) {
        jdbc.update("UPDATE task SET int_exit_status=? WHERE pk_task=?", exitStatus, task.taskId)
    }

    override fun incrementAssetCounters(task: TaskId, counts: AssetCounters): Boolean {
        return jdbc.update(ASSET_COUNTS_INC,
                counts.created,
                counts.warnings,
                counts.errors,
                counts.replaced,
                task.taskId) == 1
    }

    override fun resetAssetCounters(task: TaskId): Boolean {
        return jdbc.update(ASSET_COUNTS_RESET, task.taskId) == 1
    }

    override fun getAll(tf: TaskFilter?): KPagedList<Task> {
        val filter = tf ?: TaskFilter()
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    private fun count(filter: TaskFilter): Long {
        val query = filter.getQuery(COUNT, true)
        return jdbc.queryForObject(query, Long::class.java, *filter.getValues(true))
    }

    override fun getAll(job: UUID, state: TaskState): List<InternalTask> {
        return jdbc.query("$GET_INTERNAL WHERE task.pk_job=? AND task.int_state=?",
                INTERNAL_MAPPER, job, state.ordinal)
    }

    override fun findOne(filter: TaskFilter): Task {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return jdbc.queryForObject(query, MAPPER, *values)
    }

    override fun isAutoRetryable(task: TaskId): Boolean {
        return jdbc.queryForObject("SELECT int_run_count <= ? FROM task where pk_task=?",
                Boolean::class.java, autoRetryLimit, task.taskId)
    }

    override fun getPendingTaskCount(): Long {
        return jdbc.queryForObject(GET_PENDING_COUNT,
                Long::class.java, JobState.Active.ordinal, TaskState.Waiting.ordinal)
    }

    override fun updatePingTime(taskId: UUID, endpoint: String): Boolean {
        return jdbc.update(UPDATE_PING,
                System.currentTimeMillis(), taskId, endpoint, TaskState.Running.ordinal) == 1
    }

    override fun getOrphans(duration: Duration): List<InternalTask> {
        val time = System.currentTimeMillis() - duration.toMillis()
        return jdbc.query(GET_ORPHANS, INTERNAL_MAPPER,
                TaskState.Running.ordinal,
                TaskState.Queued.ordinal,
                time)
    }

    companion object {

        private val START_STATES = setOf(TaskState.Running)

        private val STOP_STATES = setOf(TaskState.Failure, TaskState.Skipped, TaskState.Success)

        private val INTERNAL_MAPPER = RowMapper { rs, _ ->
            InternalTask(rs.getObject("pk_task") as UUID,
                    rs.getObject("pk_job") as UUID,
                    rs.getString("str_name"),
                    TaskState.values()[rs.getInt("int_state")])
        }

        private val MAPPER = RowMapper { rs, _ ->
            Task(rs.getObject("pk_task") as UUID,
                    rs.getObject("pk_job") as UUID,
                    rs.getObject("project_id") as UUID,
                    rs.getString("str_name"),
                    TaskState.values()[rs.getInt("int_state")],
                    rs.getString("str_host"),
                    rs.getLong("time_started"),
                    rs.getLong("time_stopped"),
                    rs.getLong("time_created"),
                    rs.getLong("time_ping"),
                    buildAssetCounts(rs))
        }

        private inline fun buildAssetCounts(rs: ResultSet): Map<String, Int> {
            val result = mutableMapOf<String, Int>()
            result["assetCreatedCount"] = rs.getInt("int_asset_create_count")
            result["assetReplacedCount"] = rs.getInt("int_asset_replace_count")
            result["assetWarningCount"] = rs.getInt("int_asset_warning_count")
            result["assetErrorCount"] = rs.getInt("int_asset_error_count")
            result["assetTotalCount"] = rs.getInt("int_asset_total_count")
            return result
        }

        private const val UPDATE_PING =
                "UPDATE " +
                    "task " +
                "SET " +
                    "time_ping=? " +
                "WHERE " +
                    "pk_task=? " +
                "AND " +
                    "str_host=? " +
                "AND " +
                    "int_state=?"

        private const val ASSET_COUNTS_INC = "UPDATE " +
                "task_stat " +
                "SET " +
                "int_asset_create_count=int_asset_create_count+?," +
                "int_asset_warning_count=int_asset_warning_count+?," +
                "int_asset_error_count=int_asset_error_count+?," +
                "int_asset_replace_count=int_asset_replace_count+? " +
                "WHERE " +
                "pk_task=?"

        private const val ASSET_COUNTS_RESET = "UPDATE " +
                "task_stat " +
                "SET " +
                "int_asset_create_count=0," +
                "int_asset_warning_count=0," +
                "int_asset_error_count=0," +
                "int_asset_replace_count=0 " +
                "WHERE " +
                "pk_task=?"

        private const val GET_INTERNAL = "SELECT " +
                "task.pk_task," +
                "task.pk_job," +
                "task.str_name," +
                "task.int_state " +
            "FROM " +
                "task "

        private const val COUNT = "SELECT COUNT(1) " +
            "FROM " +
                "task " +
            "INNER JOIN " +
                "job ON (task.pk_job = job.pk_job) "

        private const val GET_ORPHANS =
                "$GET_INTERNAL " +
                "WHERE " +
                    "task.int_state IN (?,?) AND task.time_ping < ? LIMIT 15"

        private val INSERT = JdbcUtils.insert("task",
                "pk_task",
                "pk_job",
                "str_name",
                "time_created",
                "time_modified",
                "time_state_change",
                "time_ping",
                "json_script::JSON",
                "int_run_count")

        private const val GET = "SELECT " +
                "task.pk_task," +
                "task.pk_parent," +
                "task.pk_job," +
                "task.str_name," +
                "task.int_state," +
                "task.int_order," +
                "task.time_started," +
                "task.time_stopped," +
                "task.time_created," +
                "task.time_ping," +
                "task.time_state_change," +
                "task.int_exit_status," +
                "task.str_host, " +
                "task.int_run_count, " +
                "task_stat.int_asset_total_count," +
                "task_stat.int_asset_create_count," +
                "task_stat.int_asset_replace_count," +
                "task_stat.int_asset_error_count," +
                "task_stat.int_asset_warning_count," +
                "job.project_id " +
                "FROM " +
                "task " +
                "JOIN task_stat ON task.pk_task = task_stat.pk_task " +
                "JOIN job ON task.pk_job = job.pk_job "

        private const val GET_PENDING_COUNT =
                "SELECT " +
                    "COUNT(1) " +
                "FROM " +
                    "job," +
                    "task " +
                "WHERE " +
                    "job.pk_job = task.pk_job " +
                "AND " +
                    "job.int_state = ? " +
                "AND " +
                    "task.int_state = ? "
    }
}

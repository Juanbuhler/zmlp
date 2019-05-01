package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.repository.LongRangeFilter
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.hasPermission
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import java.util.*

/**
 * A TaskError describes an error generated by an asset during processing.  TaskErrors are not cleared
 * when an asset is re-processed cleanly, they persist until cleared or the job is cleared.
 *
 * @property id The unique ID of the TaskError.
 * @property taskId The task that generated the error.
 * @property jobId The job task that generated the error.
 * @property assetId The asset that generated the error.
 * @property path The file path or URI that was being processed.
 * @property message The error message from the exception that generated the error.
 * @property processor The processor in which the error occurred.
 * @property fatal If the error was fatal or not.
 * @property analyst The hostname of the analyst the task was running on. The analyst ID is not used since
 * they cycle out quickly due to auto-scaling.
 * @property phase The phase at which the error occurred: generate, execute, teardown.
 * @property timeCreated The time a which the error was created in millis since epoch.
 * @property stackTrace The full stack trace from the error, if any. This can be null.
 *
 */
class TaskError(
        val id: UUID,
        val taskId: UUID,
        val jobId: UUID,
        val assetId: UUID?,
        val path: String?,
        val message: String,
        val processor: String?,
        val fatal: Boolean,
        val analyst: String,
        val phase: String,
        val timeCreated: Long,
        val stackTrace: List<StackTraceElement>?=null)

/**
 * Describes a single line of a stack trace.
 *
 * @property file The name of the source file containing the execution point.
 * @property lineNumber The line number of the source line containing the execution point.
 * @property className The module or className of the source line containing the execution point.
 * @property methodName The name of the method containing the execution point.
 */
class StackTraceElement(
        val file: String,
        val lineNumber: Int,
        val className: String,
        val methodName: String
)

/**
 * Defines all the ways in which a TaskErrorFilter can be queried.
 *
 * @property ids An array of [TaskError] ids.
 * @property jobIds: An array of [Job] ids.
 * @property taskIds: An array of [Task] ids.
 * @property assetIds: An array of [Asset] ids.
 * @property processors: An array of [Processor] ids.
 * @property timeCreated: A [LongRangeFilter] with millis since epoch.
 * @property organizationIds: An array of [Organization] ids.
 * @property keywords A keyword query string.
 *
 */
class TaskErrorFilter(
        var ids : List<UUID>? = null,
        var jobIds: List<UUID>? = null,
        var taskIds: List<UUID>? = null,
        var assetIds: List<UUID>? = null,
        val paths: List<String>? = null,
        val processors: List<String>? = null,
        val timeCreated: LongRangeFilter?=null,
        val organizationIds : List<UUID>?=null,
        val keywords:String?=null) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
            "id" to "pk_task_error",
            "taskId" to "pk_task",
            "jobId" to "pk_job",
            "assetId" to "pk_asset",
            "path" to "str_path",
            "processors" to "str_processor",
            "timeCreated" to "time_created")

    @JsonIgnore
    override fun build() {

        if (sort == null) {
            sort = listOf("timeCreated:desc")
        }

        if (hasPermission("zorroa::superadmin")) {
            organizationIds?.let {
                addToWhere(JdbcUtils.inClause("job.pk_organization", it.size))
                addToValues(it)
            }
        }
        else {
            addToWhere("job.pk_organization=?")
            addToValues(getOrgId())
        }

        ids?.let  {
            addToWhere(JdbcUtils.inClause("task_error.pk_task_error", it.size))
            addToValues(it)
        }

        jobIds?.let {
            addToWhere(JdbcUtils.inClause("task_error.pk_job", it.size))
            addToValues(it)
        }

        taskIds?.let {
            addToWhere(JdbcUtils.inClause("task_error.pk_task", it.size))
            addToValues(it)
        }

        assetIds?.let {
            addToWhere(JdbcUtils.inClause("task_error.pk_asset", it.size))
            addToValues(it)
        }

        paths?.let {
            addToWhere(JdbcUtils.inClause("task_error.str_path", it.size))
            addToValues(it)
        }

        processors?.let {
            addToWhere(JdbcUtils.inClause("task_error.str_processor", it.size))
            addToValues(it)
        }

        timeCreated?.let {
            addToWhere(JdbcUtils.rangeClause("task_error.time_created", it))
            addToValues(it.getFilterValues())
        }

        keywords?.let {
            addToWhere("fti_keywords @@ to_tsquery(?)")
            addToValues(it)
        }
    }
}

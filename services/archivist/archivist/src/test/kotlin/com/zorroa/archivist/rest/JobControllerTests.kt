package com.zorroa.archivist.rest

import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.TaskError
import com.zorroa.archivist.domain.TaskErrorEvent
import com.zorroa.archivist.domain.TaskEvent
import com.zorroa.archivist.domain.TaskEventType
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.repository.TaskErrorDao
import com.zorroa.archivist.service.JobService
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobFilter
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.JobState
import com.zorroa.archivist.domain.JobUpdateSpec
import com.zorroa.archivist.domain.TaskSpec
import com.zorroa.archivist.domain.TaskState
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JobControllerTests : MockMvcTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var taskErrorDao: TaskErrorDao

    lateinit var job: Job

    @Before
    fun init() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        job = jobService.create(spec)
    }

    @Test
    fun testGet() {

        val result = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/jobs/" + job.id)
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val t1 = deserialize(result, Job::class.java)
        assertEquals(job.id, t1.id)
    }

    @Test
    fun testCreate() {
        val spec = JobSpec(
            "test_job_2",
            emptyZpsScript("test"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        val result = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/jobs")
                .headers(admin())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val t1 = deserialize(result, Job::class.java)
        assertEquals(spec.name, t1.name)
        assertEquals(1, t1.taskCounts!!["tasksTotal"])
        assertEquals(1, t1.taskCounts!!["tasksWaiting"])
    }
    
    @Test
    fun testUpdate() {
        val spec = JobUpdateSpec("silly_bazilly", 5, true, System.currentTimeMillis(), 5)

        val result = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/jobs/${job.id}")
                .headers(admin())
                .content(Json.serialize(spec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val t1 = deserialize(result, Job::class.java)
        assertEquals(spec.name, t1.name)
        assertEquals(spec.priority, t1.priority)
        assertEquals(spec.maxRunningTasks, t1.maxRunningTasks)
    }

    @Test
    fun testCancel() {

        val result = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/jobs/${job.id}/_cancel")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val job = jobService.get(job.id)
        assertEquals(JobState.Cancelled, job.state)

        val status = deserialize(result, Json.GENERIC_MAP)
        assertEquals("Job", status["type"])
        assertEquals("cancel", status["op"])
        assertEquals(true, status["success"])
    }

    @Test
    fun testRestart() {
        jobService.setJobState(job, JobState.Cancelled, null)

        val result = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/jobs/${job.id}/_restart")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val job = jobService.get(job.id)
        assertEquals(JobState.Active, job.state)

        val status = deserialize(result, Json.GENERIC_MAP)
        assertEquals("Job", status["type"])
        assertEquals("restart", status["op"])
        assertEquals(true, status["success"])
    }

    @Test
    fun testRetryAllFailures() {
        val t = jobService.createTask(job, TaskSpec("foo", emptyZpsScript("bar")))
        assertTrue(jobService.setTaskState(t, TaskState.Failure, null))

        val result = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/jobs/${job.id}/_retryAllFailures")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val status = deserialize(result, Json.GENERIC_MAP)
        assertEquals("Job", status["type"])
        assertEquals("retryAllFailures", status["op"])
        assertEquals(true, status["success"])

        val t2 = jobService.getTask(t.id)
        assertEquals(TaskState.Waiting, t2.state)
    }

    @Test
    @Throws(Exception::class)
    fun testGetTaskErrors() {

        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        val job = jobService.create(spec)
        val task = jobService.createTask(job, TaskSpec("foo", emptyZpsScript("bar")))

        authenticateAsAnalyst()
        val error = TaskErrorEvent(
            UUID.randomUUID(), "/foo/bar.jpg",
            "it broke", "com.zorroa.OfficeIngestor", true, "execute"
        )
        val event = TaskEvent(TaskEventType.ERROR, task.id, job.id, error)
        taskErrorDao.create(task, error)

        val result = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/jobs/${job.id}/taskerrors")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val content = result.response.contentAsString
        val log = Json.Mapper.readValue<KPagedList<TaskError>>(content,
            object : TypeReference<KPagedList<TaskError>>() {})
        assertEquals(1, log.size())
    }

    @Test
    @Throws(Exception::class)
    fun testSearch() {
        val jobs = resultForPostContent<KPagedList<Job>>(
            "/api/v1/jobs/_search",
            JobFilter()
        )
        assertTrue(jobs.size() > 0)
    }

    @Test
    fun testFindOneWithEmptyFilter() {
        val job = resultForPostContent<Job>(
            "/api/v1/jobs/_findOne",
            JobFilter()
        )
        assertEquals("test_job", job.name)
    }

    @Test
    fun testFindOneWithFilter() {
        val spec = jobSpec("baz")
        jobService.create(spec)
        val job = resultForPostContent<Job>(
            "/api/v1/jobs/_findOne",
            JobFilter(names = listOf("baz_job"))
        )
        assertEquals(spec.name, job.name)
    }

    private fun jobSpec(name: String): JobSpec {
        return JobSpec(
            "${name}_job",
            emptyZpsScript("${name}_script"),
            args = mutableMapOf("${name}_arg" to 1),
            env = mutableMapOf("${name}_env_var" to "${name}_env_value")
        )
    }
}

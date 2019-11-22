package com.zorroa.archivist.service

import com.nhaarman.mockito_kotlin.whenever
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AnalystSpec
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobPriority
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.LockState
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.domain.TaskErrorFilter
import com.zorroa.archivist.domain.TaskExpandEvent
import com.zorroa.archivist.domain.TaskState
import com.zorroa.archivist.domain.TaskStatsEvent
import com.zorroa.archivist.domain.TaskStoppedEvent
import com.zorroa.archivist.domain.ZpsScript
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.mock.zany
import com.zorroa.archivist.repository.AnalystDao
import com.zorroa.archivist.repository.TaskDao
import com.zorroa.archivist.repository.TaskErrorDao
import com.zorroa.archivist.security.InternalThreadAuthentication
import com.zorroa.archivist.security.Perm
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.security.withAuth
import io.micrometer.core.instrument.MeterRegistry
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@TestPropertySource(locations = ["classpath:gcs-test.properties"])
class GCPDispatcherServiceTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var dispatchQueueManager: DispatchQueueManager

    @Autowired
    lateinit var fileStorageService: FileStorageService

    fun launchJob(priority: Int): Job {
        val spec1 = JobSpec(
            "test_job_p$priority",
            emptyZpsScript("priority_$priority"),
            priority = priority
        )
        return jobService.create(spec1)
    }

    @Test
    fun testGetNext() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        jobService.create(spec)

        val storage = FileStorage(
            "foo", "gs://foo/bar/bing.jpg", "fs", "image/jpeg", fileServerProvider
        )

        whenever(fileStorageService.get(zany(FileStorageSpec::class.java))).thenReturn(storage)
        whenever(fileStorageService.getSignedUrl(zany(), zany(), anyLong(), zany())).thenReturn("https://foo/bar")

        authenticateAsAnalyst()
        val next = dispatchQueueManager.getNext()
        assertNotNull(next)
        assertEquals(next?.logFile, "https://foo/bar")
    }
}

class DispatcherServiceTests : AbstractTest() {

    @Autowired
    lateinit var analystDao: AnalystDao

    @Autowired
    lateinit var taskDao: TaskDao

    @Autowired
    lateinit var taskErrorDao: TaskErrorDao

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var dispatcherService: DispatcherService

    @Autowired
    lateinit var dispatchQueueManager: DispatchQueueManager

    @Autowired
    lateinit var meterRegistry: MeterRegistry

    @Test
    fun testHandleStatsEvent() {
        val stats = listOf(TaskStatsEvent("zplugins.core.TestProcessor", 10, 0.1, 0.5, 0.25))
        dispatcherService.handleStatsEvent(stats)

        assertEquals(
            0.1, meterRegistry.timer(
                "zorroa.processor.process_min_time",
                "processor", "zplugins.core.TestProcessor"
            ).totalTime(TimeUnit.SECONDS)
        )

        assertEquals(
            0.5, meterRegistry.timer(
                "zorroa.processor.process_max_time",
                "processor", "zplugins.core.TestProcessor"
            ).totalTime(TimeUnit.SECONDS)
        )

        assertEquals(
            0.25, meterRegistry.timer(
                "zorroa.processor.process_avg_time",
                "processor", "zplugins.core.TestProcessor"
            ).totalTime(TimeUnit.SECONDS)
        )
    }

    @Test
    fun getTaskPriority() {
        var priority = dispatcherService.getDispatchPriority()
        assertTrue(priority.isEmpty())

        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        jobService.create(spec)

        priority = dispatcherService.getDispatchPriority()
        assertEquals(1, priority.size)

        jdbc.update("UPDATE job_count SET int_task_state_1=100")

        // The org should have priority of 100 now.
        priority = dispatcherService.getDispatchPriority()
        assertEquals(100, priority[0].priority)
    }

    @Test
    fun getTaskPriorityMultipleOrganizations() {

        val spec1 = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        val job = jobService.create(spec1)
        jdbc.update(
            "UPDATE job_count SET int_task_state_1=100 WHERE pk_job=?",
            job.id
        )

        val pspec = ProjectSpec("foojam", projectId = UUID.randomUUID())
        val project = projectService.create(pspec)

        withAuth(InternalThreadAuthentication(project.id, listOf(Perm.STORAGE_CREATE))) {
            val spec2 = JobSpec(
                "test_job",
                emptyZpsScript("foo"),
                args = mutableMapOf("foo" to 1),
                env = mutableMapOf("foo" to "bar")
            )
            jobService.create(spec2)
        }

        val priority = dispatcherService.getDispatchPriority()
        assertEquals(0, priority[0].priority)
        assertEquals(100, priority[1].priority)
    }

    @Test
    fun testGetNextWithInteractivePriortity() {
        val analyst = "https://127.0.0.1:5000"

        // Standard job is launched first, which should go first
        launchJob(JobPriority.Standard)
        Thread.sleep(2)
        // A higher priority job is launched, now it goes first.
        val job = launchJob(JobPriority.Interactive)

        authenticateAsAnalyst()
        val next = dispatchQueueManager.getNext()
        assertNotNull(next)
        next?.let {
            assertEquals(job.id, it.jobId)
            val host: String = this.jdbc.queryForObject(
                "SELECT str_host FROM task WHERE pk_task=?",
                String::class.java, it.id
            )
            assertEquals(analyst, host)
        }
    }

    @Test
    fun testGetNext() {
        val analyst = "https://127.0.0.1:5000"
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )
        val job = jobService.create(spec)

        authenticateAsAnalyst()
        val next = dispatchQueueManager.getNext()

        assertNotNull(next)
        next?.let {
            assertEquals(job.id, it.jobId)
            val host: String = this.jdbc.queryForObject(
                "SELECT str_host FROM task WHERE pk_task=?",
                String::class.java, it.id
            )
            assertEquals(analyst, host)
        }
    }

    @Test
    fun testGetNextFailureMaxRunningJob() {
        val analyst = "https://127.0.0.1:5000"
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar"),
            maxRunningTasks = 0
        )
        jobService.create(spec)
        authenticateAsAnalyst()
        val next = dispatchQueueManager.getNext()
        assertNull(next)
    }

    @Test
    fun testGetNextLockedAnalyst() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo")
        )
        jobService.create(spec)

        authenticateAsAnalyst()
        val analyst = "https://127.0.0.1:5000"
        val aspec = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "0.42.0",
            null
        ).apply { endpoint = analyst }

        val node = analystDao.create(aspec)
        assertTrue(analystDao.setLockState(node, LockState.Locked))

        val next = dispatchQueueManager.getNext()
        assertNull(next)
    }

    @Test
    fun testStartAndStopTask() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo")
        )
        jobService.create(spec)

        authenticateAsAnalyst()
        val analyst = "https://127.0.0.1:5000"
        val aspec = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "0.42.0",
            null
        ).apply { endpoint = analyst }

        val node = analystDao.create(aspec)

        val next = dispatchQueueManager.getNext()
        assertNotNull(next)
        next?.let {
            assertTrue(dispatcherService.startTask(it))
            assertNotNull(analystDao.get(analyst).taskId)
            assertTrue(dispatcherService.stopTask(it, TaskStoppedEvent(0)))
            assertNull(analystDao.get(analyst).taskId)
        }
    }

    // make run
    @Test
    fun testStopErrorTaskManualKill() {
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()

        val doc1 = Asset(id1)
        doc1.setAttr("source.path", "/foo/bar.jpg")

        val doc2 = Asset(id2)
        doc2.setAttr("source.path", "/flim/flam.jpg")

        val spec = JobSpec(
            "test_job",
            ZpsScript(
                "foo",
                generate = null,
                execute = null,
                assets = listOf(doc1, doc2)
            )
        )
        jobService.create(spec)

        authenticateAsAnalyst()
        val analyst = "https://127.0.0.1:5000"
        val aspec = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "0.42.0",
            null
        ).apply { endpoint = analyst }

        val next = dispatchQueueManager.getNext()
        assertNotNull(next)
        next?.let {
            assertTrue(dispatcherService.startTask(it))
            assertTrue(dispatcherService.stopTask(it, TaskStoppedEvent(-9, manualKill = true)))
            authenticate()
            assertEquals(TaskState.Failure, taskDao.get(next.taskId).state)
        }
    }

    @Test
    fun testStopErrorAutoRetry() {
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()

        val doc1 = Asset(id1)
        doc1.setAttr("source.path", "/foo/bar.jpg")

        val doc2 = Asset(id2)
        doc2.setAttr("source.path", "/flim/flam.jpg")

        val spec = JobSpec(
            "test_job",
            ZpsScript(
                "foo",
                generate = null,
                execute = null,
                assets = listOf(doc1, doc2)
            )
        )
        jobService.create(spec)

        authenticateAsAnalyst()
        val analyst = "https://127.0.0.1:5000"
        val aspec = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "0.42.0",
            null
        ).apply { endpoint = analyst }

        val next = dispatchQueueManager.getNext()
        assertNotNull(next)
        next?.let {
            assertTrue(dispatcherService.startTask(it))
            assertTrue(dispatcherService.stopTask(it, TaskStoppedEvent(1, manualKill = false)))
            authenticate()
            assertEquals(TaskState.Waiting, taskDao.get(next.taskId).state)
        }
    }

    @Test
    fun testStopErrorPastAutoRetryLimit() {
        val id1 = UUID.randomUUID().toString()
        val id2 = UUID.randomUUID().toString()

        val doc1 = Asset(id1)
        doc1.setAttr("source.path", "/foo/bar.jpg")

        val doc2 = Asset(id2)
        doc2.setAttr("source.path", "/flim/flam.jpg")

        val spec = JobSpec(
            "test_job",
            ZpsScript(
                "foo",
                generate = null,
                execute = null,
                assets = listOf(doc1, doc2)
            )
        )
        jobService.create(spec)

        authenticateAsAnalyst()
        val analyst = "https://127.0.0.1:5000"
        val aspec = AnalystSpec(
            1024,
            648,
            1024,
            0.5f,
            "0.42.0",
            null
        ).apply { endpoint = analyst }

        val next = dispatchQueueManager.getNext()
        assertNotNull(next)
        next?.let {
            // Set run count above the retry limit.
            jdbc.update("UPDATE task SET int_run_count=4 WHERE pk_task=?", next.taskId)
            assertTrue(dispatcherService.startTask(it))
            assertTrue(dispatcherService.stopTask(it, TaskStoppedEvent(1, manualKill = false)))
            authenticate()
            assertEquals(TaskState.Failure, taskDao.get(next.taskId).state)
            assertEquals(2, taskErrorDao.getAll(TaskErrorFilter(jobIds = listOf(next.jobId))).size())
            assertEquals(2, taskErrorDao.getAll(TaskErrorFilter(taskIds = listOf(next.taskId))).size())
            assertEquals(2, taskErrorDao.getAll(TaskErrorFilter()).size())
        }
    }

    @Test
    fun testExpandFromParentTask() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        val job = jobService.create(spec)
        val zps = emptyZpsScript("bar")
        zps.execute = mutableListOf(ProcessorRef("foo", "bar"))

        val task1 = dispatcherService.getWaitingTasks(getProjectId(), 1)
        val task2 = dispatcherService.expand(
            task1[0],
            TaskExpandEvent(listOf(AssetSpec("http://foo/123.jpg")))
        )
        val zps2 = taskDao.getScript(task2.id)

        assertNotNull(zps2.execute)
        // Validate task2 inherited from task
        assertEquals(1, zps.execute!!.size)
    }

    fun launchJob(priority: Int): Job {
        val spec1 = JobSpec(
            "test_job_p$priority",
            emptyZpsScript("priority_$priority"),
            priority = priority
        )
        return jobService.create(spec1)
    }
}

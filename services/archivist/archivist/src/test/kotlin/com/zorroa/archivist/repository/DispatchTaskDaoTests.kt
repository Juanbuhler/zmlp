package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.Job
import com.zorroa.archivist.domain.JobPriority
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.TaskExpandEvent
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.service.DispatcherService
import com.zorroa.archivist.service.JobService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DispatchTaskDaoTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var dispatcherService: DispatcherService

    @Autowired
    lateinit var dispatchTaskDao: DispatchTaskDao

    @Test
    fun testScriptGlobalArgsAreSet() {
        // args from ztool get merged into script.
        launchJob(JobPriority.Standard)
        val tasks = dispatchTaskDao.getNextByJobPriority(JobPriority.Standard, 5)
        assertTrue(tasks.isNotEmpty())
        tasks.forEach {
            assertEquals(it.script.globalArgs!!["captain"], "kirk")
        }
    }

    @Test
    fun testGetByJobPriority() {
        launchJob(JobPriority.Standard)
        val job2 = launchJob(JobPriority.Interactive)
        val job3 = launchJob(JobPriority.Reindex)

        var tasks = dispatchTaskDao.getNextByJobPriority(JobPriority.Interactive, 5)

        assertEquals(2, tasks.size)
        assertEquals(job3.id, tasks[0].jobId)
        assertEquals(job2.id, tasks[1].jobId)
    }

    @Test
    fun testGetNextByOrgSortedByJobPriority() {
        val job1 = launchJob(JobPriority.Standard)
        val job2 = launchJob(JobPriority.Interactive)
        val job3 = launchJob(JobPriority.Reindex)

        var tasks = dispatchTaskDao.getNextByProject(getProjectId(), 5)

        assertEquals(job3.id, tasks[0].jobId)
        assertEquals(job2.id, tasks[1].jobId)
        assertEquals(job1.id, tasks[2].jobId)
    }

    @Test
    fun testGetNextByOrgNextSortedByTime() {
        val job1 = launchJob(JobPriority.Standard)
        val job2 = launchJob(JobPriority.Standard)
        val job3 = launchJob(JobPriority.Standard)

        var tasks = dispatchTaskDao.getNextByProject(getProjectId(), 5)

        assertEquals(job1.id, tasks[0].jobId)
        assertEquals(job2.id, tasks[1].jobId)
        assertEquals(job3.id, tasks[2].jobId)

        dispatcherService.expand(tasks[0], TaskExpandEvent(listOf(AssetSpec("http://foo/123.jpg"))))
        Thread.sleep(2)
        dispatcherService.expand(tasks[1], TaskExpandEvent(listOf(AssetSpec("http://foo/123.jpg"))))
        Thread.sleep(2)
        dispatcherService.expand(tasks[2], TaskExpandEvent(listOf(AssetSpec("http://foo/123.jpg"))))

        tasks = dispatchTaskDao.getNextByProject(getProjectId(), 6)

        // Job that was launched first goes first.
        assertEquals(job1.id, tasks[0].jobId)
        assertEquals(job1.id, tasks[1].jobId)
        assertEquals(job2.id, tasks[2].jobId)
        assertEquals(job2.id, tasks[3].jobId)
        assertEquals(job3.id, tasks[4].jobId)
        assertEquals(job3.id, tasks[5].jobId)
    }

    @Test
    fun testGetNextByOrg() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        jobService.create(spec)
        val tasks = dispatchTaskDao.getNextByProject(getProjectId(), 5)
        assertEquals(1, tasks.size)
        assertTrue(tasks[0].args.containsKey("foo"))
        assertEquals(spec.env, tasks[0].env)
    }

    @Test
    fun testGetTaskPriority() {
        val spec = JobSpec(
            "test_job",
            emptyZpsScript("foo"),
            args = mutableMapOf("foo" to 1),
            env = mutableMapOf("foo" to "bar")
        )

        assertTrue(dispatchTaskDao.getDispatchPriority().isEmpty())

        jobService.create(spec)

        val priority = dispatchTaskDao.getDispatchPriority()[0]
        assertEquals(getProjectId(), priority.projectId)
        assertEquals(0, priority.priority)
    }

    fun launchJob(priority: Int): Job {
        val spec1 = JobSpec(
            "test_job_p$priority",
            emptyZpsScript("priority_$priority"),
            args = mutableMapOf("captain" to "kirk"),
            priority = priority
        )
        return jobService.create(spec1)
    }
}

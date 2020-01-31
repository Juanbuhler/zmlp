package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AssetCounters
import com.zorroa.archivist.domain.CredentialsSpec
import com.zorroa.archivist.domain.CredentialsType
import com.zorroa.archivist.domain.JobFilter
import com.zorroa.archivist.domain.JobSpec
import com.zorroa.archivist.domain.JobState
import com.zorroa.archivist.domain.JobType
import com.zorroa.archivist.domain.JobUpdateSpec
import com.zorroa.archivist.domain.emptyZpsScript
import com.zorroa.archivist.service.CredentialsService
import com.zorroa.archivist.service.JobService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class JobDaoTests : AbstractTest() {

    @Autowired
    lateinit var jobDao: JobDao

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var credentialsService: CredentialsService

    @Test
    fun testCreate() {

        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args = mutableMapOf("foo" to 1),
                env = mutableMapOf("foo" to "bar"))

        val t1 = jobDao.create(spec, JobType.Import)
        assertEquals(spec.name, t1.name)
        assertEquals(JobState.InProgress, t1.state) // no tasks
        assertEquals(JobType.Import, t1.type)
    }

    @Test
    fun testResumePausedJobs() {
        assertEquals(0, jobDao.resumePausedJobs())

        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                paused = true,
                pauseDurationSeconds = 1L)
        val job1 = jobDao.create(spec, JobType.Import)
        assertTrue(job1.paused)

        Thread.sleep(1001)
        assertEquals(1, jobDao.resumePausedJobs())
        val job2 = jobDao.get(job1.id)
        assertEquals(job1.id, job2.id)
        assertFalse(job2.paused)
    }

    @Test
    fun testUpdate() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args = mutableMapOf("foo" to 1),
                env = mutableMapOf("foo" to "bar"))
        val t1 = jobDao.create(spec, JobType.Import)
        val update = JobUpdateSpec("bilbo_baggins", 5, true, System.currentTimeMillis(), 5)
        assertTrue(jobDao.update(t1, update))
        val t2 = jobDao.get(t1.id)
        assertEquals(update.name, t2.name)
        assertEquals(update.priority, t2.priority)
        assertEquals(update.paused, t2.paused)
        assertEquals(update.timePauseExpired, t2.timePauseExpired)
        assertEquals(update.maxRunningTasks, t2.maxRunningTasks)
    }

    @Test
    fun testSetTimeStarted() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args = mutableMapOf("foo" to 1),
                env = mutableMapOf("foo" to "bar"))
        val t1 = jobDao.create(spec, JobType.Import)
        assertTrue(jobDao.setTimeStarted(t1))
        assertFalse(jobDao.setTimeStarted(t1))
        val time = jdbc.queryForObject("SELECT time_started FROM job WHERE pk_job=?", Long::class.java, t1.jobId)
        assertTrue(time != -1L)
    }

    @Test
    fun testGet() {
        val spec = JobSpec("test_job",
                emptyZpsScript("test_script"),
                args = mutableMapOf("foo" to 1),
                env = mutableMapOf("foo" to "bar"))

        val t2 = jobDao.create(spec, JobType.Import)
        val t1 = jobDao.get(t2.id)

        assertEquals(t2.name, t1.name)
        assertEquals(t2.projectId, t1.projectId)
        assertEquals(t2.state, t1.state)
        assertEquals(t2.type, t1.type)
    }

    @Test
    fun getTestForClient() {
        val spec = JobSpec("test_job",
                emptyZpsScript("test_script"),
                args = mutableMapOf("foo" to 1),
                env = mutableMapOf("foo" to "bar"))

        val t2 = jobDao.create(spec, JobType.Import)
        val t1 = jobDao.get(t2.id, forClient = true)
        assertNotNull(t1.assetCounts)
        assertNotNull(t1.taskCounts)
    }

    @Test
    fun getTestWeHaveTimestampsForClient() {
        val spec = JobSpec("test_job",
                emptyZpsScript("test_script"),
                args = mutableMapOf("foo" to 1),
                env = mutableMapOf("foo" to "bar"))

        val t2 = jobDao.create(spec, JobType.Import)
        val t1 = jobDao.get(t2.id, forClient = true)
        assertNotNull(t1.timeStarted)
        assertNotNull(t1.timeUpdated)
    }

    @Test
    fun testIncrementAssetStats() {
        val spec = JobSpec("test_job",
                emptyZpsScript("test_script"),
                args = mutableMapOf("foo" to 1),
                env = mutableMapOf("foo" to "bar"))

        val counters = AssetCounters(
                total = 10,
                errors = 6,
                replaced = 4,
                warnings = 2,
                created = 6)

        val job1 = jobDao.create(spec, JobType.Import)
        assertTrue(jobDao.incrementAssetCounters(job1, counters))
        val map = jdbc.queryForMap("SELECT * FROM job_stat WHERE pk_job=?", job1.id)

        assertEquals(counters.created, map["int_asset_create_count"])
        assertEquals(counters.replaced, map["int_asset_replace_count"])
        assertEquals(counters.errors, map["int_asset_error_count"])
        assertEquals(counters.warnings, map["int_asset_warning_count"])
        assertEquals(counters.total, map["int_asset_total_count"])
    }

    @Test
    fun testGetAllWithFilter() {
        val orgId = UUID.randomUUID()
        for (i in 1..10) {
            val spec = JobSpec("run_some_stuff_$i",
                    emptyZpsScript("test_script"))
            jobDao.create(spec, JobType.Import)
        }

        var filter = JobFilter(names = listOf("run_some_stuff_1"))
        var jobs = jobDao.getAll(filter)
        assertEquals(1, jobs.size())
        assertEquals(1, jobs.page.totalCount)

        filter = JobFilter(paused = true)
        jobs = jobDao.getAll(filter)
        assertEquals(0, jobs.size())
    }

    @Test
    fun testAllSortColumns() {
        for (i in 1..10) {
            val random = Random.nextInt(1, 100000)
            val spec = JobSpec("run_some_stuff_$random",
                    emptyZpsScript("test_script"))
            jobDao.create(spec, JobType.Import)
        }

        // All the columns we can sort by.
        val sortFields = listOf(
            "id", "type", "name", "timeCreated", "state", "priority", "projectId", "dataSourceId"
        )

        // Just test the DB allows us to sort
        for (field in sortFields) {
            var filter = JobFilter().apply {
                sort = listOf("$field:a")
            }
            val page = jobDao.getAll(filter)
            assertTrue(page.size() > 0)
        }
    }

    @Test
    fun testDelete() {
        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args = mutableMapOf("foo" to 1),
                env = mutableMapOf("foo" to "bar"))

        val job = jobDao.create(spec, JobType.Import)
        assertTrue(jobDao.delete(job))
        assertFalse(jobDao.delete(job))
    }

    @Test
    fun testGetExpired() {
        assertTrue(jobDao.getExpired(1, TimeUnit.DAYS, 100).isEmpty())

        val spec = JobSpec("test_job",
                emptyZpsScript("foo"),
                args = mutableMapOf("foo" to 1),
                env = mutableMapOf("foo" to "bar"))

        val job = jobDao.create(spec, JobType.Import)
        assertTrue(jobDao.setState(job, JobState.Failure, null))
        Thread.sleep(100)
        assertTrue(jobDao.getExpired(99, TimeUnit.MILLISECONDS, 100).isNotEmpty())
        assertTrue(jobDao.getExpired(1, TimeUnit.DAYS, 100).isEmpty())
    }

    @Test
    fun testSetState() {
        val spec = JobSpec("test_job", emptyZpsScript("foo"))
        var job = jobDao.create(spec, JobType.Import)
        assertFalse(jobDao.setState(job, JobState.InProgress, null))
        assertTrue(jobDao.setState(job, JobState.Cancelled, null))
        job = jobDao.get(job.id)
        assertTrue(job.timeStopped > -1)
        assertTrue(jobDao.setState(job, JobState.InProgress, null))
        job = jobDao.get(job.id)
        assertEquals(job.timeStopped, -1L)
    }

    @Test
    fun testSetCredentials() {
        val creds = credentialsService.create(
            CredentialsSpec("test",
                CredentialsType.AWS, """{"foo": "bar"}""")
        )

        val spec = JobSpec("test_job", emptyZpsScript("foo"))
        var job = jobDao.create(spec, JobType.Import)
        jobDao.setCredentials(job, listOf(creds))

        assertEquals(1, jdbc.queryForObject(
            "SELECT COUNT(1) FROM x_credentials_job WHERE pk_job=?",
            Int::class.java, job.jobId))
    }

    @Test
    fun testGetCredentialsTypes() {
        val creds = credentialsService.create(
            CredentialsSpec("test",
                CredentialsType.AWS, """{"foo": "bar"}""")
        )

        val spec = JobSpec("test_job", emptyZpsScript("foo"))
        var job = jobDao.create(spec, JobType.Import)
        jobDao.setCredentials(job, listOf(creds))

        assertTrue(jobDao.getCredentialsTypes(job).contains("AWS"))
    }
}

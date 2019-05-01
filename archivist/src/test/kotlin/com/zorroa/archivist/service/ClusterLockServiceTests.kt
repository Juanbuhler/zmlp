package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.ClusterLockSpec
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.atomic.LongAdder
import javax.annotation.PostConstruct
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClusterLockExecutorTests : AbstractTest() {

    @Autowired
    lateinit var clusterLockService: ClusterLockService

    lateinit var textExecutor: ClusterLockExecutor

    @PostConstruct
    fun init() {
        /**
         * Create a new ThreadPoolTaskExecutor for testing lock semantics because
         * the global one is configured for a single thread in tests.
         */
        val tp = ThreadPoolTaskExecutor()
        tp.corePoolSize = 8
        tp.maxPoolSize = 8
        tp.threadNamePrefix = "[TEST-QUEUE]"
        tp.isDaemon = true
        tp.setQueueCapacity(1000)
        tp.initialize()

        textExecutor = ClusterLockExecutorImpl(clusterLockService, tp)
    }

    @Test
    fun testLaunchSoftLock() {
        val count = LongAdder()

        textExecutor.execute(ClusterLockSpec.softLock("counter")) {
            count.increment()
            Thread.sleep(1000)
        }

        // Will be skipped because counter is already running.
        textExecutor.execute(ClusterLockSpec.softLock("counter")) {
            count.increment()
        }

        Thread.sleep(1000)
        assertEquals(1, count.toInt())
    }

    @Test
    fun testLaunchHardLock() {

        val count = LongAdder()

        // Both of these run serially, one after another.

        textExecutor.execute(ClusterLockSpec.hardLock("counter")) {
            count.increment()
        }

        textExecutor.execute(ClusterLockSpec.hardLock("counter")) {
            count.increment()
        }

        Thread.sleep(500)
        assertEquals(2, count.toInt())
    }

    @Test
    fun testCombinableLock() {

        val count = LongAdder()

        textExecutor.execute(ClusterLockSpec.combineLock("counter")) {
            count.increment()
            Thread.sleep(100)
        }

        // Sleep here so the first launch is running
        Thread.sleep(50)

        textExecutor.execute(ClusterLockSpec.combineLock("counter")) {
            // look mom, no code!
        }

        Thread.sleep(500)
        assertEquals(2, count.toInt())
    }

    @Test
    fun testAsyncLock() {

        val count = LongAdder()

        val lock1 = textExecutor.submit(ClusterLockSpec.hardLock("counter")) {
            count.increment()
            count.toInt()
        }

        val lock2 = textExecutor.submit(ClusterLockSpec.hardLock("counter")) {
            count.increment()
            count.toInt()
        }

        // long tasks

        val value1 = lock1.get()
        val value2 = lock2.get()

        assertTrue(value1 ?: 0 > 0)
        assertTrue(value2 ?: 0 > 0)
        assertEquals(2, count.toInt())
    }

    @Test
    fun testReentrantLock() {

        val count = LongAdder()

        val lock1 = textExecutor.execute(ClusterLockSpec.hardLock("counter")) {
            count.increment()
            val lock2 = textExecutor.execute(ClusterLockSpec.hardLock("counter")) {
                count.increment()
            }
        }
        Thread.sleep(1000)
        assertEquals(2, count.toInt())
    }

    @Test
    fun testInlineLock() {
        val count = LongAdder()
        val lock1 = textExecutor.inline(ClusterLockSpec.hardLock("counter")) {
            count.increment()
            count.toInt()
        }
        assertEquals(1, lock1)
    }

    @Test
    fun testInlineReentrantock() {
        val count = LongAdder()
        textExecutor.inline(ClusterLockSpec.hardLock("counter")) {
            count.increment()
            textExecutor.inline(ClusterLockSpec.hardLock("counter")) {
                count.increment()
            }
        }
        assertEquals(2, count.toInt())
    }

    @Test
    fun testSubmitWithFailedLock() {

        val count = LongAdder()

        // Setup a lock that will be around for about 1 second.
        textExecutor.execute(ClusterLockSpec.softLock("counter")) {
            count.increment()
            Thread.sleep(1000)
        }

        // Now, submit and async soft which fails immediately, thus returning null.
        val result = textExecutor.submit(ClusterLockSpec.softLock("counter")) {
            "test"
        }.get()

        assertNull(result)
    }
}
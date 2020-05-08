package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Category
import com.zorroa.archivist.domain.ModType
import com.zorroa.archivist.domain.PipelineMod
import com.zorroa.archivist.domain.PipelineModSpec
import com.zorroa.archivist.domain.Provider
import com.zorroa.archivist.service.PipelineModService
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PipelineModDaoTests : AbstractTest() {

    @Autowired
    lateinit var pipelineModDao: PipelineModDao

    @Autowired
    lateinit var pipelineModService: PipelineModService

    lateinit var module: PipelineMod

    @Before
    fun init() {
        val spec = PipelineModSpec("foo", "test",
            Provider.ZORROA,
            Category.ZORROA_STD,
            ModType.LABEL_DETECTION,
            listOf(), listOf(), true)
        module = pipelineModService.create(spec)
    }

    @Test
    fun testGet() {
        val mod1 = pipelineModDao.get(module.id)
        assertEquals(module.id, mod1.id)
    }

    @Test
    fun testDelete() {
        assertTrue(pipelineModDao.delete(module))
    }

    @Test
    fun testGetProjectBased() {
        val spec = PipelineModSpec("foo2", "test",
            Provider.ZORROA,
            Category.ZORROA_STD,
            ModType.LABEL_DETECTION,
            listOf(), listOf(), false)

        val mod1 = pipelineModService.create(spec)
        val mod2 = pipelineModDao.get(mod1.id)
        assertEquals(mod1.id, mod2.id)
    }
    
    @Test
    fun testFindByIdIn() {
        assertTrue(pipelineModDao.findByIdIn(listOf(UUID.randomUUID())).isEmpty())
        assertFalse(pipelineModDao.findByIdIn(listOf(module.id)).isEmpty())
    }

    @Test
    fun testFindNameIn() {
        assertTrue(pipelineModDao.findByNameIn(listOf("12345")).isEmpty())
        assertFalse(pipelineModDao.findByNameIn(listOf(module.name)).isEmpty())
    }

    @Test
    fun findByName() {
        assertNull(pipelineModDao.findByName("cat", true))
        assertNotNull(pipelineModDao.findByName("foo", true))
    }
}

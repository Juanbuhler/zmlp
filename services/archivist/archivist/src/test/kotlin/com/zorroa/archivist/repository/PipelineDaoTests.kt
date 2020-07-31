package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Category
import com.zorroa.archivist.domain.ModelObjective
import com.zorroa.archivist.domain.PipelineFilter
import com.zorroa.archivist.domain.PipelineModSpec
import com.zorroa.archivist.domain.PipelineSpec
import com.zorroa.archivist.domain.PipelineUpdate
import com.zorroa.archivist.domain.Provider
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.service.PipelineModService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import kotlin.test.assertEquals

class PipelineDaoTests : AbstractTest() {

    @Autowired
    lateinit var pipelineDao: PipelineDao

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @PersistenceContext
    lateinit var entityManager: EntityManager

    val pipelineName = "unit"

    @Test
    fun testCreateAndGetByName() {
        val pl1 = PipelineSpec(pipelineName)
        val pl2 = pipelineDao.create(pl1)
        val pl3 = pipelineDao.get(pipelineName)
        assertEquals(pl2.id, pl3.id)
    }

    @Test
    fun testCreateAndGetById() {
        val pl1 = PipelineSpec(pipelineName)
        val pl2 = pipelineDao.create(pl1)
        val pl3 = pipelineDao.get(pl2.id)
        assertEquals(pl2.id, pl3.id)
    }

    @Test
    fun testGetDefault() {
        val pipeline = pipelineDao.getDefault()
        val defaultId = jdbc.queryForObject(
            "SELECT pk_pipeline_default FROM project WHERE pk_project=?", UUID::class.java, getProjectId()
        )
        assertEquals(pipeline.id, defaultId)
    }

    @Test
    fun testSetPipelineMods() {
        val pl1 = PipelineSpec(pipelineName)
        val pl2 = pipelineDao.create(pl1)

        val mod = pipelineModService.create(
            PipelineModSpec(
                "test",
                "1234",
                Provider.ZORROA,
                Category.ZORROA_STD,
                ModelObjective.LABEL_DETECTION,
                listOf(),
                listOf(),
                true
            )
        )
        entityManager.flush()

        pipelineDao.setPipelineMods(pl2.id, listOf(mod))
        val count = jdbc.queryForObject(
            "SELECT COUNT(1) FROM x_module_pipeline WHERE pk_pipeline=?", Int::class.java, pl2.id
        )
        assertEquals(1, count)
    }

    @Test
    fun testUpdate() {
        val pl1 = pipelineDao.create(PipelineSpec("import-test2"))
        val updated = PipelineUpdate("hello", listOf(), listOf())

        pipelineDao.update(pl1.id, updated)
        val pl2 = pipelineDao.get("hello")
        assertEquals(pl1.id, pl2.id)
    }

    @Test
    fun testCount() {
        val count = pipelineDao.count(PipelineFilter())
        pipelineDao.create(PipelineSpec(pipelineName))
        assertEquals(pipelineDao.count(PipelineFilter()), count + 1)
    }
}

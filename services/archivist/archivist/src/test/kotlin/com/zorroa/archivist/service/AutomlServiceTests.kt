package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AutomlSession
import com.zorroa.archivist.domain.AutomlSessionSpec
import com.zorroa.archivist.domain.AutomlSessionState
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelType
import com.zorroa.archivist.repository.AutomlDao
import com.zorroa.zmlp.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AutomlServiceTests : AbstractTest() {

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var automlService: AutomlService

    @Autowired
    lateinit var automlDao: AutomlDao

    @Autowired
    lateinit var pipelineModService: PipelineModService

    @Test
    fun testCreate() {
        val automl = create()

        assertNull(automl.automlModel)
        assertEquals("project/foo/region/us-central/datasets/foo", automl.automlDataSet)
        assertEquals("/foo/bar", automl.automlTrainingJob)
    }

    @Test
    fun testCheckAutomlTrainingStatus() {
        val session = create()
        automlService.checkAutomlTrainingStatus()

        val mod = pipelineModService.getByName("animals")
        assertTrue("/model/name" in Json.serializeToString(mod))

        val session2 = automlDao.getOne(session.id)
        assertEquals(AutomlSessionState.FINISHED, session2.state)
        assertEquals("/model/name", session2.automlModel)
    }

    fun create(): AutomlSession {
        val modelSpec = ModelSpec("animals", ModelType.GCP_LABEL_DETECTION)
        val model = modelService.createModel(modelSpec)

        val automlSpec = AutomlSessionSpec(
            "project/foo/region/us-central/datasets/foo",
            "/foo/bar"
        )

        return automlService.createSession(model, automlSpec)
    }
}

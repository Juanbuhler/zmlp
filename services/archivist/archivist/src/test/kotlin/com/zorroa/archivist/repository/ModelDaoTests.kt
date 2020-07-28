package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.ModelFilter
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelType
import com.zorroa.archivist.service.ModelService
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelDaoTests : AbstractTest() {

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var modelJdbcDao: ModelJdbcDao

    @Test
    fun testMarkAsReady() {
        val model = modelService.createModel(ModelSpec("foo", ModelType.ZVI_LABEL_DETECTION))
        modelJdbcDao.markAsReady(model.id, true)
        var trained = jdbc.queryForObject(
            "SELECT bool_trained FROM model WHERE pk_model=?", Boolean::class.java, model.id
        )
        assertTrue(trained)

        modelJdbcDao.markAsReady(model.id, false)
        trained = jdbc.queryForObject(
            "SELECT bool_trained FROM model WHERE pk_model=?", Boolean::class.java, model.id
        )
        assertFalse(trained)
    }


    @Test
    fun testFindOrderDesc(){
        val model1 = modelService.createModel(ModelSpec("test1", ModelType.ZVI_LABEL_DETECTION))
        Thread.sleep(100)
        val model2 = modelService.createModel(ModelSpec("test2", ModelType.ZVI_LABEL_DETECTION))
        Thread.sleep(100)
        val model3 = modelService.createModel(ModelSpec("test3", ModelType.ZVI_LABEL_DETECTION))

        val find = modelJdbcDao.find(ModelFilter())

        assertEquals(model3.id, find[0].id)
        assertEquals(model2.id, find[1].id)
        assertEquals(model1.id, find[2].id)
    }
}

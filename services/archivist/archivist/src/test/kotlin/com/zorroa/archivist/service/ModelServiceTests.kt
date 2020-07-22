package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Asset
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.JobState
import com.zorroa.archivist.domain.ModOpType
import com.zorroa.archivist.domain.Model
import com.zorroa.archivist.domain.ModelApplyRequest
import com.zorroa.archivist.domain.ModelFilter
import com.zorroa.archivist.domain.ModelSpec
import com.zorroa.archivist.domain.ModelTrainingArgs
import com.zorroa.archivist.domain.ModelType
import com.zorroa.archivist.domain.ProcessorRef
import com.zorroa.archivist.security.getProjectId
import com.zorroa.zmlp.util.Json
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.EmptyResultDataAccessException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ModelServiceTests : AbstractTest() {

    @Autowired
    lateinit var modelService: ModelService

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var assetSearchService: AssetSearchService

    val testSearch =
        """{"query": {"term": { "source.filename": "large-brown-cat.jpg"} } }"""

    fun create(): Model {
        val mspec = ModelSpec(
            "test",
            ModelType.ZVI_LABEL_DETECTION,
            deploySearch = Json.Mapper.readValue(testSearch, Json.GENERIC_MAP)
        )
        return modelService.createModel(mspec)
    }

    @Test
    fun testCreateModel() {
        val model = create()
        assertModel(model)
    }

    @Test
    fun createFaceModelDefaultModuleName() {
        val mspec = ModelSpec(
            "faces",
            ModelType.ZVI_FACE_RECOGNITION,
            deploySearch = Json.Mapper.readValue(testSearch, Json.GENERIC_MAP)
        )
        val model = modelService.createModel(mspec)
        assertEquals("zvi-face-recognition", model.moduleName)
    }

    @Test
    fun createFaceModuleCustomModel() {
        val mspec = ModelSpec(
            "faces",
            ModelType.ZVI_FACE_RECOGNITION,
            moduleName = "foo",
            deploySearch = Json.Mapper.readValue(testSearch, Json.GENERIC_MAP)
        )
        val model = modelService.createModel(mspec)
        assertEquals("foo", model.moduleName)
    }

    @Test
    fun testGetModel() {
        val model1 = create()
        val model2 = modelService.getModel(model1.id)
        assertEquals(model2, model2)
        assertModel(model1)
        assertModel(model2)
    }

    @Test
    fun testTrainModel() {
        val model1 = create()
        val job = modelService.trainModel(model1, ModelTrainingArgs())

        assertEquals(model1.trainingJobName, job.name)
        assertEquals(JobState.InProgress, job.state)
        assertEquals(1, job.priority)
    }

    @Test(expected = EmptyResultDataAccessException::class)
    fun testFineOneNotFound() {
        modelService.findOne(ModelFilter(names = listOf("abc")))
    }

    @Test
    fun testFindOne() {
        val model1 = create()
        val filter = ModelFilter(
            names = listOf(model1.name),
            ids = listOf(model1.id),
            types = listOf(model1.type)
        )
        val model2 = modelService.findOne(filter)
        assertEquals(model1, model2)
        assertModel(model2)
    }

    @Test
    fun testFind() {
        val model1 = create()
        val filter = ModelFilter(
            names = listOf(model1.name),
            ids = listOf(model1.id),
            types = listOf(model1.type)
        )
        filter.sort = filter.sortMap.keys.map { "$it:a" }
        val all = modelService.find(filter)
        assertEquals(1, all.size())
        assertEquals(model1, all.list[0])
        assertModel(all.list[0])
    }

    @Test
    fun testPublishModel() {
        val model1 = create()
        val mod = modelService.publishModel(model1)
        Json.prettyPrint(mod)
        Json.prettyPrint(model1)
        assertEquals(getProjectId(), mod.projectId)
        assertEquals(model1.moduleName, mod.name)
        assertEquals("Zorroa", mod.provider)
        assertEquals("Custom Models", mod.category)
        assertEquals("Label Detection", mod.type)
        assertEquals(ModOpType.APPEND, mod.ops[0].type)
    }

    @Test
    fun testPublishModelUpdate() {
        val model1 = create()
        val mod1 = modelService.publishModel(model1)
        val mod2 = modelService.publishModel(model1)

        assertEquals(mod1.id, mod2.id)
        assertEquals(mod1.name, mod2.name)

        val op1 = Json.Mapper.convertValue(mod1.ops[0].apply, ProcessorRef.LIST_OF)
        val op2 = Json.Mapper.convertValue(mod2.ops[0].apply, ProcessorRef.LIST_OF)

        // make sure it versioned up.
        assertNotNull(op1[0].args?.get("version"))
        assertNotNull(op2[0].args?.get("version"))

        assertNotEquals(
            op1[0].args?.get("version"),
            op2[0].args?.get("version")
        )
    }

    @Test
    fun testDeployModelDefaults() {
        setupTestAsset()

        val model1 = create()
        modelService.publishModel(model1)

        val rsp = modelService.deployModel(model1, ModelApplyRequest())
        val tasks = jobService.getTasks(rsp.job!!.id)
        val script = jobService.getZpsScript(tasks.list[0].id)
        assertEquals("Deploying model: test", script.name)
        assertEquals(1, script.generate!!.size)
        assertEquals("zmlp_core.core.generators.AssetSearchGenerator", script.generate!![0].className)
    }

    @Test
    fun testDeployModelCustomSearch() {
        setupTestAsset()

        val model1 = create()
        modelService.publishModel(model1)

        val rsp = modelService.deployModel(model1, ModelApplyRequest(search = Model.matchAllSearch))
        val tasks = jobService.getTasks(rsp.job!!.id)
        val script = jobService.getZpsScript(tasks.list[0].id)

        val scriptstr = Json.prettyString(script)
        assertTrue("\"match_all\" : { }" in scriptstr)
        assertTrue("modelId" in scriptstr)
    }

    @Test
    fun excludeLabeledAssetsFromSearch() {
        val model = create()

        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg", label = model.getLabel("husky"))
        val rsp = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val asset = assetService.getAsset(rsp.created[0])
        assetService.index(asset.id, asset.document, true)
        refreshIndex()

        val counts = modelService.getLabelCounts(model)
        assertEquals(1, counts["husky"])

        assertEquals(1, assetSearchService.count(Model.matchAllSearch))
        assertEquals(
            0,
            assetSearchService.count(
                modelService.wrapSearchToExcludeTrainingSet(model, Model.matchAllSearch)
            )
        )
    }

    fun setupTestAsset(): Asset {
        val spec = AssetSpec(
            "gs://cats/large-brown-cat.jpg",
            mapOf("aux.pet" to "dog")
        )
        val req = BatchCreateAssetsRequest(assets = listOf(spec))
        val rsp = assetService.batchCreate(req)
        val asset = assetService.getAsset(rsp.created[0])
        assetService.index(asset.id, asset.document, true)
        refreshIndex()
        return asset
    }

    @Test
    fun getLabelCounts() {
        val model = create()
        val specs = listOf(
            AssetSpec("https://i.imgur.com/12abc.jpg", label = model.getLabel("beaver")),
            AssetSpec("https://i.imgur.com/abc123.jpg", label = model.getLabel("ant")),
            AssetSpec("https://i.imgur.com/horse.jpg", label = model.getLabel("horse")),
            AssetSpec("https://i.imgur.com/zani.jpg", label = model.getLabel("zanzibar"))
        )

        assetService.batchCreate(
            BatchCreateAssetsRequest(specs)
        )

        val counts = modelService.getLabelCounts(model)
        assertEquals(1, counts["ant"])
        assertEquals(1, counts["horse"])
        assertEquals(1, counts["beaver"])
        assertEquals(1, counts["zanzibar"])

        val keys = counts.keys.toList()
        assertEquals("ant", keys[0])
        assertEquals("zanzibar", keys[3])
    }

    fun assertModel(model: Model) {
        assertEquals(ModelType.ZVI_LABEL_DETECTION, model.type)
        assertEquals("test", model.name)
        assertEquals("test", model.moduleName)
        assertTrue(model.fileId.endsWith("model.zip"))
        assertFalse(model.ready)
    }
}

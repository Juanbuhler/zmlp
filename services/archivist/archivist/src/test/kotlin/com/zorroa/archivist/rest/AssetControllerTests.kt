package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.AssetFileLocator
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchUploadAssetsRequest
import com.zorroa.archivist.domain.ProjectStorageCategory
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.service.AssetSearchService
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.zmlp.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.io.File

class AssetControllerTests : MockMvcTest() {

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Autowired
    lateinit var assetSearchService: AssetSearchService

    @Test
    fun testBatchCreate() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_batch_create")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(mapOf("assets" to listOf(spec))))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    @Test
    fun testDelete() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val id = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec))).created[0]

        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v3/assets/$id")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$._id", CoreMatchers.equalTo(id)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.result", CoreMatchers.equalTo("deleted")))
            .andReturn()
    }

    @Test
    fun testDeleteByQuery() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))

        val payload = """{
                "query": {
                    "match_all": { }
                }
            }
        """.trimIndent()

        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v3/assets/_delete_by_query")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(payload)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.total", CoreMatchers.equalTo(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.deleted", CoreMatchers.equalTo(1)))
            .andReturn()
    }

    @Test
    fun testBatchIndex_withError() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val batch = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val id = batch.created[0]

        val brokenPayload = """{
                "$id": {
                    "doc": {
                        "source": {
                            "filename": "cats.png"
                        }
                    }
                }
            }
        """.trimIndent()

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_batch_index")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(brokenPayload)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()", CoreMatchers.equalTo(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].index._id", CoreMatchers.equalTo(id)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].index.status", CoreMatchers.equalTo(400)))
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.items[0].index.error.reason",
                    CoreMatchers.containsString("strict_dynamic_mapping_exception")
                )
            )
            .andReturn()
    }

    @Test
    fun testBatchIndex() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val batch = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val id = batch.created[0]
        val asset = assetService.getAsset(id)
        asset.setAttr("aux.captain", "kirk")
        val payload = mapOf(asset.id to asset.document)

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_batch_index")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(payload))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.errors", CoreMatchers.equalTo(false)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items.length()", CoreMatchers.equalTo(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].index._id", CoreMatchers.equalTo(asset.id)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.items[0].index._shards.failed",
                CoreMatchers.equalTo(0)))
    }

    @Test
    fun testIndex() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val created = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val id = created.created[0]
        val asset = assetService.getAsset(id)

        val rsp = mvc.perform(
            MockMvcRequestBuilders.put("/api/v3/assets/$id/_index")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serializeToString(asset.document))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$._id", CoreMatchers.equalTo(id)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.result", CoreMatchers.equalTo("updated")))
            .andReturn()
    }

    @Test
    fun testUpdate() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val created = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val id = created.created[0]

        val update = """{
                "doc": {
                    "source": {
                        "filename": "cats.png"
                    }
                }
            }
        """.trimIndent()

        val res = mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/$id/_update")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(update)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.result", CoreMatchers.equalTo("updated")))
            .andExpect(MockMvcResultMatchers.jsonPath("$._id", CoreMatchers.equalTo(id)))
            .andReturn()
    }

    @Test
    fun testUpdateByQuery() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))

        val update = """{
                "query": {
                    "match_all": { }
                },
                "script": {
                    "source": "ctx._source['source']['filename'] = 'test.png'"
                }
            }
        """.trimIndent()

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_update_by_query")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(update)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.updated", CoreMatchers.equalTo(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.batches", CoreMatchers.equalTo(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.version_conflicts", CoreMatchers.equalTo(0)))
            .andReturn()
    }

    @Test
    fun testBatchUpdateWithDoc() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val created = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val id = created.created[0]

        val update = """{
                "$id": {
                    "doc": {
                        "source": {
                            "filename": "dogs.png"
                        }
                    }
                }
            }
        """.trimIndent()

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_batch_update")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(update)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    @Test
    fun testGet() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val created = assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))
        val id = created.created[0]

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/assets/$id")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.id", CoreMatchers.equalTo(id)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.document", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testBatchUpload() {

        val file = MockMultipartFile(
            "files", "file-name.data", "image/jpeg",
            File("src/test/resources/test-data/toucan.jpg").inputStream().readBytes()
        )

        val body = MockMultipartFile(
            "body", "",
            "application/json",
            """{"assets":[{"uri": "src/test/resources/test-data/toucan.jpg"}]}""".toByteArray()
        )

        val rsp = mvc.perform(
            multipart("/api/v3/assets/_batch_upload")
                .file(body)
                .file(file)
                .headers(admin())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.created[0]", CoreMatchers.anything()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.failed.length()", CoreMatchers.equalTo(0)))
            .andReturn()
    }

    @Test
    fun testStreamSourceFile() {
        val batchUpload = BatchUploadAssetsRequest(
            assets = listOf(AssetSpec("/foo/bar/toucan.jpg"))
        )
        batchUpload.files = arrayOf(
            MockMultipartFile(
                "files", "toucan.jpg", "image/jpeg",
                File("src/test/resources/test-data/toucan.jpg").inputStream().readBytes()
            )
        )

        val rsp = assetService.batchUpload(batchUpload)
        val id = rsp.created[0]

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/assets/$id/_stream")
                .headers(admin())
                .contentType(MediaType.IMAGE_JPEG_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.IMAGE_JPEG_VALUE))
            .andReturn()
    }

    @Test
    fun testSteamFile() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        val rsp = assetService.batchCreate(
            BatchCreateAssetsRequest(
                assets = listOf(spec)
            )
        )
        val id = rsp.created[0]
        val loc = AssetFileLocator(id, ProjectStorageCategory.PROXY, "bob.jpg")
        val storage = ProjectStorageSpec(loc, mapOf("cats" to 100), "test".toByteArray())
        projectStorageService.store(storage)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/assets/$id/_files/proxy/bob.jpg")
                .headers(admin())
                .contentType(MediaType.IMAGE_JPEG_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.IMAGE_JPEG_VALUE))
            .andReturn()
    }

    @Test
    fun testUploadFile() {

        val file = MockMultipartFile(
            "file", "toucan.jpg", "image/jpeg",
            File("src/test/resources/test-data/toucan.jpg").inputStream().readBytes()
        )

        val body = MockMultipartFile(
            "body", "",
            "application/json",
            "{\"category\": \"proxy\", \"name\": \"toucan.jpg\", \"attrs\": {\"foo\": \"bar\"}}".toByteArray()

        )

        val rsp = assetService.batchCreate(BatchCreateAssetsRequest(
            assets = listOf(AssetSpec("https://i.imgur.com/SSN26nN.jpg"))
        ))
        val id = rsp.created[0]

        mvc.perform(
            multipart("/api/v3/assets/$id/_files")
                .file(body)
                .file(file)
                .headers(admin())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.category", CoreMatchers.equalTo("proxy")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo("toucan.jpg")))
            .andExpect(MockMvcResultMatchers.jsonPath("$.size", CoreMatchers.equalTo(97221)))
            .andReturn()
    }

    @Test
    fun testSearchNullBody() {
        val spec = AssetSpec("https://i.imgur.com/SSN26nN.jpg")
        assetService.batchCreate(BatchCreateAssetsRequest(listOf(spec)))

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v3/assets/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.hits.total.value", CoreMatchers.equalTo(1)))
            .andExpect(
                MockMvcResultMatchers.jsonPath(
                    "$.hits.hits[0]._source.source.path",
                    CoreMatchers.equalTo("https://i.imgur.com/SSN26nN.jpg")
                )
            )
            .andReturn()
    }

    @Test
    fun testSearchWithQuerySize() {

        assetService.batchCreate(
            BatchCreateAssetsRequest(
                listOf(
                    AssetSpec("https://i.imgur.com/SSN26nN.jpg"),
                    AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
                )
            )
        )

        val search = """{ "size": 1, "query": { "match_all": {}} }"""

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(search)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.hits.total.value", CoreMatchers.equalTo(2)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.hits.hits.length()", CoreMatchers.equalTo(1)))
            .andReturn()
    }

    @Test
    fun testSearchWithScroll() {

        assetService.batchCreate(
            BatchCreateAssetsRequest(
                listOf(
                    AssetSpec("https://i.imgur.com/SSN26nN.jpg"),
                    AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
                )
            )
        )

        val search = """{ "size": 1, "query": { "match_all": {}} }"""

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_search?scroll=1m")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(search)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$._scroll_id", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testScroll() {

        assetService.batchCreate(
            BatchCreateAssetsRequest(
                listOf(
                    AssetSpec("https://i.imgur.com/SSN26nN.jpg"),
                    AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
                )
            )
        )

        val search = """{ "size": 1, "query": { "match_all": {}} }"""

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v3/assets/_search?scroll=1m")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(search)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$._scroll_id", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testClearScroll() {
        assetService.batchCreate(
            BatchCreateAssetsRequest(
                listOf(
                    AssetSpec("https://i.imgur.com/SSN26nN.jpg"),
                    AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
                )
            )
        )

        val search = assetSearchService.search(mapOf(), mapOf("scroll" to arrayOf("1m")))
        val body = """
            {"scroll_id": "${search.scrollId}" }
        """.trimIndent()
        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v3/assets/_search/scroll")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(body)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.succeeded", CoreMatchers.equalTo(true)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.num_freed", CoreMatchers.anything()))
            .andReturn()
    }
}

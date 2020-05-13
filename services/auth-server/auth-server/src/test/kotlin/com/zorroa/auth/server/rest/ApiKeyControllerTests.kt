package com.zorroa.auth.server.rest

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.zorroa.auth.server.MockMvcTest
import com.zorroa.auth.server.domain.ApiKeyFilter
import com.zorroa.auth.server.domain.ApiKeySpec
import com.zorroa.auth.server.domain.ProjectApiKeysEnabledSpec
import com.zorroa.zmlp.apikey.Permission
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.util.UUID

class ApiKeyControllerTests : MockMvcTest() {

    val json = jacksonObjectMapper()

    @Test
    fun testCreate() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead)
        )
        val pid = UUID.randomUUID()
        mvc.perform(
            MockMvcRequestBuilders.post("/auth/v1/apikey")
                .headers(superAdmin(pid))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json.writeValueAsBytes(spec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.projectId", CoreMatchers.equalTo(pid.toString())))
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("test")))
            .andExpect(
                jsonPath(
                    "$.permissions[0]",
                    CoreMatchers.containsString("AssetsRead")
                )
            )
            .andReturn()
    }

    @Test
    fun testCreateFail() {

        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead)
        )

        val pid = UUID.randomUUID()

        mvc.perform(
            MockMvcRequestBuilders.post("/auth/v1/apikey")
                .headers(superAdmin(pid))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json.writeValueAsBytes(spec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.projectId", CoreMatchers.equalTo(pid.toString())))
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("test")))

        mvc.perform(
            MockMvcRequestBuilders.post("/auth/v1/apikey")
                .headers(superAdmin(pid))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json.writeValueAsBytes(spec))
        )
            .andExpect(MockMvcResultMatchers.status().isConflict)
            .andExpect(jsonPath("$.error", CoreMatchers.equalTo("DataIntegrityViolation")))
            .andReturn()
    }

    @Test
    fun testUpdate() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead)
        )

        val create = apiKeyService.create(spec)

        val specUpdated = ApiKeySpec(
            "testUpdated",
            setOf(Permission.AssetsRead)
        )

        mvc.perform(
            MockMvcRequestBuilders.put("/auth/v1/apikey/${create.id}")
                .headers(superAdmin(create.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json.writeValueAsBytes(specUpdated))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.projectId", CoreMatchers.equalTo(create.projectId.toString())))
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("testUpdated")))
            .andExpect(
                jsonPath(
                    "$.permissions[0]",
                    CoreMatchers.containsString("AssetsRead")
                )
            )
            .andReturn()
    }

    @Test
    fun testCreate_rsp_403() {
        val spec = ApiKeySpec(
            "test",
            setOf(Permission.AssetsRead)
        )

        mvc.perform(
            MockMvcRequestBuilders.post("/auth/v1/apikey")
                .headers(standardUser(mockKey))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json.writeValueAsBytes(spec))
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
    }

    @Test
    fun testGet() {
        mvc.perform(
            MockMvcRequestBuilders.get("/auth/v1/apikey/${mockKey.id}")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("standard-key")))
            .andExpect(
                jsonPath(
                    "$.permissions[0]",
                    CoreMatchers.containsString("AssetsRead")
                )
            )
            .andReturn()
    }

    @Test
    fun testGetNotFound() {
        mvc.perform(
            MockMvcRequestBuilders.get("/auth/v1/apikey/${UUID.randomUUID()}")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isNotFound)
            .andReturn()
    }

    @Test
    fun testFindOne() {
        val filter = ApiKeyFilter(names = listOf("standard-key"))

        mvc.perform(
            MockMvcRequestBuilders.get("/auth/v1/apikey/_findOne")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json.writeValueAsBytes(filter))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("standard-key")))
            .andExpect(
                jsonPath(
                    "$.permissions[0]",
                    CoreMatchers.containsString("AssetsRead")
                )
            )
            .andReturn()
    }

    @Test
    fun testSearch() {
        val filter = ApiKeyFilter(names = listOf("standard-key"))
        filter.sort = listOf("name:asc")

        mvc.perform(
            MockMvcRequestBuilders.get("/auth/v1/apikey/_search")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json.writeValueAsBytes(filter))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.list[0].name", CoreMatchers.equalTo("standard-key")))
            .andExpect(
                jsonPath(
                    "$.list[0].permissions[0]",
                    CoreMatchers.containsString("AssetsRead")
                )
            )
            .andReturn()
    }

    @Test
    fun testFindOne_rsp_404() {
        val filter = ApiKeyFilter(names = listOf("mrcatlady"))

        mvc.perform(
            MockMvcRequestBuilders.get("/auth/v1/apikey/_findOne")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json.writeValueAsBytes(filter))
        )
            .andExpect(MockMvcResultMatchers.status().`is`(404))
            .andReturn()
    }

    @Test
    fun testDownload() {
        mvc.perform(
            MockMvcRequestBuilders.get("/auth/v1/apikey/${mockKey.id}/_download")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.header().exists("Content-disposition"))
            .andReturn()
    }

    @Test
    fun testGetAll() {
        mvc.perform(
            MockMvcRequestBuilders.get("/auth/v1/apikey")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(jsonPath("$.[0]name", CoreMatchers.equalTo("standard-key")))
            .andExpect(
                jsonPath(
                    "$.[0]permissions[0]",
                    CoreMatchers.containsString("AssetsRead")
                )
            )
            .andReturn()
    }

    @Test
    fun testDelete() {
        mvc.perform(
            MockMvcRequestBuilders.delete("/auth/v1/apikey/${mockKey.id}")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }

    @Test
    fun testUpdateEnabledByProject() {
        var projectApiKeysEnabledSpec = ProjectApiKeysEnabledSpec(false)
        mvc.perform(
            MockMvcRequestBuilders.post("/auth/v1/project/enabled")
                .headers(superAdmin(mockKey.projectId))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(json.writeValueAsBytes(projectApiKeysEnabledSpec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }
}

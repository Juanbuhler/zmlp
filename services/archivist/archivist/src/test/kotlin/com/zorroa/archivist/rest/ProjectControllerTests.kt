package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.domain.ProjectQuotaCounters
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.repository.ProjectQuotasDao
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.zmlp.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.Date

class ProjectControllerTests : MockMvcTest() {

    lateinit var testProject: Project
    lateinit var testSpec: ProjectSpec

    @Autowired
    lateinit var projectStorageService: ProjectStorageService

    @Autowired
    lateinit var projectQuotasDao: ProjectQuotasDao

    @Before
    fun init() {
        testSpec = ProjectSpec("Zorroa Test")
        testProject = projectService.create(testSpec)
    }

    @Test
    fun testCreate() {
        val testSpec = ProjectSpec("Zorroa Test2")
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/projects")
                .headers(admin())
                .content(Json.serialize(testSpec))
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo(testSpec.name)))
            .andReturn()
    }

    @Test
    fun testGet() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/projects/${testProject.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo(testProject.name)))
            .andExpect(jsonPath("$.id", CoreMatchers.equalTo(testProject.id.toString())))
            .andExpect(jsonPath("$.timeCreated", CoreMatchers.anything()))
            .andExpect(jsonPath("$.timeModified", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testGetAll() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/projects/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.list.length()", CoreMatchers.equalTo(2)))
            .andExpect(jsonPath("$.page.totalCount", CoreMatchers.equalTo(2)))
            .andExpect(jsonPath("$.page.from", CoreMatchers.equalTo(0)))
            .andReturn()
    }

    @Test
    fun findOne() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/projects/_findOne")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(mapOf("ids" to listOf(testProject.id))))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo(testProject.name)))
            .andExpect(jsonPath("$.id", CoreMatchers.equalTo(testProject.id.toString())))
            .andExpect(jsonPath("$.timeCreated", CoreMatchers.anything()))
            .andExpect(jsonPath("$.timeModified", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun getSettings() {
        val pid = getProjectId()
        val settings = projectService.getSettings(pid)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/projects/$pid/_settings")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonPath(
                    "$.defaultPipelineId",
                    CoreMatchers.equalTo(settings.defaultPipelineId.toString())
                )
            )
            .andExpect(
                jsonPath(
                    "$.defaultIndexRouteId",
                    CoreMatchers.equalTo(settings.defaultIndexRouteId.toString())
                )
            )
            .andReturn()
    }

    @Test
    fun updateSettings() {
        val pid = getProjectId()
        val settings = projectService.getSettings(pid)

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/projects/$pid/_settings")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(settings))
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonPath(
                    "$.defaultPipelineId",
                    CoreMatchers.equalTo(
                        settings.defaultPipelineId.toString()
                    )
                )
            )
            .andExpect(
                jsonPath(
                    "$.defaultIndexRouteId",
                    CoreMatchers.equalTo(settings.defaultIndexRouteId.toString())
                )
            )
            .andReturn()
    }

    @Test
    fun testGetMyProject() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/project")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name", CoreMatchers.equalTo("unittest")))
            .andReturn()
    }

    @Test
    fun testGetMyProjectSettings() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/project/_settings")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.defaultPipelineId", CoreMatchers.anything()))
            .andExpect(jsonPath("$.defaultIndexRouteId", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testGetMyProjecQuotas() {
        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/project/_quotas")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.videoSecondsMax", CoreMatchers.anything()))
            .andExpect(jsonPath("$.videoSecondsCount", CoreMatchers.anything()))
            .andExpect(jsonPath("$.pageMax", CoreMatchers.anything()))
            .andExpect(jsonPath("$.pageCount", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun testGetMyProjecQuotasTimeSeries() {
        val counters = ProjectQuotaCounters()
        counters.videoClipCount = 1
        counters.pageCount = 1
        counters.videoLength = 10.0
        counters.imageFileCount = 1
        counters.documentFileCount = 1
        counters.videoFileCount = 1

        projectQuotasDao.incrementTimeSeriesCounters(Date(), counters)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/project/_quotas_time_series")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].videoSecondsCount", CoreMatchers.anything()))
            .andExpect(jsonPath("$[0].pageCount", CoreMatchers.anything()))
            .andReturn()
    }

    @Test
    fun updateMySettings() {
        val pid = getProjectId()
        val settings = projectService.getSettings(pid)

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/project/_settings")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(settings))
        )
            .andExpect(status().isOk)
            .andExpect(
                jsonPath(
                    "$.defaultPipelineId",
                    CoreMatchers.equalTo(
                        settings.defaultPipelineId.toString()
                    )
                )
            )
            .andExpect(
                jsonPath(
                    "$.defaultIndexRouteId",
                    CoreMatchers.equalTo(settings.defaultIndexRouteId.toString())
                )
            )
            .andReturn()
    }

    @Test
    fun enableProject() {
        val pid = getProjectId()
        val settings = projectService.getSettings(pid)

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/project/$pid/_enable")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(settings))
        )
            .andExpect(status().isOk)
            .andReturn()
    }

    @Test
    fun disableProject() {
        val pid = getProjectId()
        val settings = projectService.getSettings(pid)

        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/project/$pid/_disable")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(settings))
        )
            .andExpect(status().isOk)
            .andReturn()
    }
}

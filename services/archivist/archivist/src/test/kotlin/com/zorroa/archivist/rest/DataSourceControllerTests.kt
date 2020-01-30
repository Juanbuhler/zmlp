package com.zorroa.archivist.rest

import com.zorroa.archivist.MockMvcTest
import com.zorroa.archivist.domain.DataSourceFilter
import com.zorroa.archivist.domain.DataSourceSpec
import com.zorroa.archivist.domain.DataSourceUpdate
import com.zorroa.archivist.service.CredentialsService
import com.zorroa.archivist.service.DataSourceService
import com.zorroa.zmlp.util.Json
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext

class DataSourceControllerTests : MockMvcTest() {

    val testSpec = DataSourceSpec(
        "Testing 123",
        "gs://foo-bar"
    )

    @Autowired
    lateinit var dataSourceService: DataSourceService

    @Autowired
    lateinit var credentialsService: CredentialsService

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Test
    fun testCreate() {
        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/data-sources")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(testSpec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.uri", CoreMatchers.equalTo(testSpec.uri)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(testSpec.name)))
            .andReturn()
    }

    @Test
    fun testUpdate() {
        val ds = dataSourceService.create(testSpec)
        val update = DataSourceUpdate("spock", "gs://foo/bar", ds.fileTypes, ds.pipelineId, setOf())
        mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/data-sources/${ds.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(update))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.uri", CoreMatchers.equalTo(update.uri)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(update.name)))
            .andReturn()
    }

    @Test
    fun testDelete() {
        val ds = dataSourceService.create(testSpec)
        mvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/data-sources/${ds.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
    }
    @Test
    fun testGet() {
        val ds = dataSourceService.create(testSpec)

        mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/data-sources/${ds.id}")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.uri", CoreMatchers.equalTo(testSpec.uri)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(testSpec.name)))
            .andReturn()
    }

    @Test
    fun testFindOne() {
        val ds = dataSourceService.create(testSpec)
        val filter = DataSourceFilter(ids = listOf(ds.id))

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/data-sources/_findOne")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(filter))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.uri", CoreMatchers.equalTo(testSpec.uri)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", CoreMatchers.equalTo(testSpec.name)))
            .andReturn()
    }

    @Test
    fun testFind() {
        val ds = dataSourceService.create(testSpec)
        val filter = DataSourceFilter(ids = listOf(ds.id))

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/data-sources/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(filter))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.list[0].uri", CoreMatchers.equalTo(testSpec.uri)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.list[0].name", CoreMatchers.equalTo(testSpec.name)))
            .andReturn()
    }

    @Test
    fun testImportAssets() {
        val ds = dataSourceService.create(testSpec)

        mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/data-sources/${ds.id}/_import")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.jsonPath("$.dataSourceId", CoreMatchers.equalTo(ds.id.toString())))
            .andReturn()
    }
}

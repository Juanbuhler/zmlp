package com.zorroa.archivist.rest

import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.archivist.domain.AuditLogEntry
import com.zorroa.archivist.domain.AuditLogFilter
import com.zorroa.archivist.domain.AuditLogType
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertTrue

class AuditLogControllerTests : MockMvcTest() {

    @Before
    fun init() {
        addTestAssets("set04/standard")
    }

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Test
    @Throws(Exception::class)
    fun testSearch() {
        val session = admin()
        val filter = AuditLogFilter(types = listOf(AuditLogType.Created))

        val result = mvc.perform(
                MockMvcRequestBuilders.post("/api/v1/auditlog/_search")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(filter)))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

        val content = result.response.contentAsString
        val log = Json.Mapper.readValue<KPagedList<AuditLogEntry>>(content)
        assertTrue(log.size() > 0)
    }
}
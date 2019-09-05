package com.zorroa.archivist.rest

import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.readValue
import com.zorroa.archivist.domain.AttrType
import com.zorroa.archivist.domain.Field
import com.zorroa.archivist.domain.FieldFilter
import com.zorroa.archivist.domain.FieldSpecExpose
import com.zorroa.archivist.domain.FieldUpdateSpec
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.Json
import org.junit.Assert
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FieldControllerTests : MockMvcTest() {

    val fieldSpec = FieldSpecExpose("Media Clip Parent", "media.clip.parent")

    @Test
    fun testExposeField() {

        val req = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/fields/_expose")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(fieldSpec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val field = Json.Mapper.readValue<Field>(req.response.contentAsString, Field::class.java)
        assertEquals(fieldSpec.name, field.name)
        assertEquals(AttrType.StringExact, field.attrType)
        assertEquals(fieldSpec.attrName, field.attrName)
    }

    @Test
    fun testDeleteField() {
        val field = fieldSystemService.createField(fieldSpec)

        val req = mvc.perform(
            MockMvcRequestBuilders.delete("/api/v1/fields/${field.id}")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(fieldSpec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val rsp = Json.Mapper.readValue<Map<String, Any>>(req.response.contentAsString)
        assertTrue(rsp["success"] as Boolean)
    }

    @Test
    fun testUpdateField() {
        val field = fieldSystemService.createField(fieldSpec)
        val updateSpec = FieldUpdateSpec(
            "test", true, true, 2.0f, false, requireList = false,
            options = listOf("a", "b", "c")
        )

        val req = mvc.perform(
            MockMvcRequestBuilders.put("/api/v1/fields/${field.id}")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(updateSpec))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val rsp = Json.Mapper.readValue<Map<String, Any>>(req.response.contentAsString)
        assertTrue(rsp["success"] as Boolean)

        val updatedField = Json.Mapper.convertValue<Field>(rsp.getValue("object"))

        Assert.assertEquals(updateSpec.name, updatedField.name)
        Assert.assertEquals(updateSpec.editable, updatedField.editable)
        Assert.assertEquals(updateSpec.keywords, updatedField.keywords)
        Assert.assertEquals(updateSpec.keywordsBoost, updatedField.keywordsBoost)
        Assert.assertEquals(updateSpec.options, updatedField.options)
    }

    @Test
    fun testGet() {
        val field = fieldSystemService.createField(fieldSpec)

        val req = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/fields/${field.id}")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = Json.Mapper.readValue<Field>(req.response.contentAsString, Field::class.java)
        assertEquals(field.id, result.id)
    }

    @Test
    fun testSearch() {
        val field = fieldSystemService.createField(fieldSpec)

        val filter = FieldFilter(ids = listOf(field.id))

        val req = mvc.perform(
            MockMvcRequestBuilders.post("/api/v1/fields/_search")
                .headers(admin())
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(filter))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = Json.Mapper.readValue<KPagedList<Field>>(
            req.response.contentAsString, Field.Companion.TypeRefKList
        )
        assertEquals(1, result.size())
        assertEquals(field.id, result[0].id)
    }

    @Test
    fun testFindOne() {
        val field = fieldSystemService.createField(fieldSpec)

        val result = resultForPostContent<Field>(
            "/api/v1/fields/_findOne",
            FieldFilter(ids = listOf(field.id))
        )
        assertEquals(field.id, result.id)
    }

    @Test
    fun testGetWithNullBody() {
        setupEmbeddedFieldSets()

        val req = mvc.perform(
            MockMvcRequestBuilders.get("/api/v1/fields/_search")
                .headers(admin())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()

        val result = Json.Mapper.readValue<KPagedList<Field>>(
            req.response.contentAsString, Field.Companion.TypeRefKList
        )
        assertTrue(result.size() > 0)
    }
}

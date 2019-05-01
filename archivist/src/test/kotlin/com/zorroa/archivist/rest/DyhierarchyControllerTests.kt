package com.zorroa.archivist.rest

import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.collect.ImmutableList
import com.zorroa.archivist.domain.DyHierarchy
import com.zorroa.archivist.domain.DyHierarchyLevel
import com.zorroa.archivist.domain.DyHierarchyLevelType
import com.zorroa.archivist.domain.DyHierarchySpec
import com.zorroa.archivist.domain.FolderSpec
import com.zorroa.archivist.domain.Source
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors

import java.text.ParseException

import org.junit.Assert.assertEquals
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * Created by chambers on 4/18/17.
 */
class DyhierarchyControllerTests : MockMvcTest() {

    @Before
    @Throws(ParseException::class)
    fun init() {
        for (f in getTestImagePath("set01").toFile().listFiles()!!) {
            if (!f.isFile || f.isHidden) {
                continue
            }
            val ab = Source(f)
            ab.setAttr("tree.path", ImmutableList.of("/foo/bar/", "/bing/bang/", "/foo/shoe/"))
            indexService.index(ab)
        }
        refreshIndex()
    }

    @Test
    @Throws(Exception::class)
    fun testCreateAndDelete() {
        val session = admin()

        val (id) = folderService.create(FolderSpec("foo"), false)
        val spec = DyHierarchySpec(id, listOf(
                DyHierarchyLevel("source.date", DyHierarchyLevelType.Day),
                DyHierarchyLevel("source.type.raw"),
                DyHierarchyLevel("source.extension.raw"),
                DyHierarchyLevel("source.filename.raw")))

        val result = mvc.perform(post("/api/v1/dyhi")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(spec)))
                .andExpect(status().isOk)
                .andReturn()

        val dh = Json.Mapper.readValue<DyHierarchy>(result.response.contentAsString)
        assertEquals(4, dh.levels.size.toLong())

        val delRsp = mvc.perform(delete("/api/v1/dyhi/${dh.id}")
                .session(session)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(status().isOk)
                .andReturn()

        val delBody = Json.Mapper.readValue<Map<String, Any>>(delRsp.response.contentAsString)
        assertEquals(delBody["success"], true)
    }
}

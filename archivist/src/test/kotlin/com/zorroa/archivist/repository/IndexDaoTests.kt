package com.zorroa.archivist.repository

import com.fasterxml.jackson.core.type.TypeReference
import com.google.common.collect.ImmutableList
import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.Source
import com.zorroa.common.clients.SearchBuilder
import com.zorroa.common.util.Json
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayOutputStream
import java.io.IOException

class IndexDaoTests : AbstractTest() {

    @Autowired
    internal lateinit var indexDao: IndexDao
    internal lateinit var asset1: Document

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Before
    fun init() {
        val builder = Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"))
        builder.setAttr("bar.str", "dog")
        builder.setAttr("bar.int", 100)
        asset1 = indexDao.index(builder)
        logger.info("Creating asset: {}", asset1.id)
        refreshIndex()
    }

    @Test
    fun testGetFieldValue() {
        assertEquals("dog", indexDao.getFieldValue(asset1.id, "bar.str"))
        assertEquals(100, (indexDao.getFieldValue<Any>(asset1.id, "bar.int") as Int).toLong())
    }

    @Test
    fun testGetById() {
        val asset2 = indexDao.get(asset1.id)
        assertEquals(asset1.id, asset2.id)
    }

    @Test
    fun testGetByPath() {
        val p = getTestImagePath("set04/standard/beer_kettle_01.jpg")
        val asset2 = indexDao.get(p)
        assertNotNull(asset2)
    }

    @Test
    fun testExistsByPath() {
        val p = getTestImagePath("set04/standard/beer_kettle_01.jpg")
        assertTrue(indexDao.exists(p))
    }

    @Test
    fun testExistsById() {
        assertTrue(indexDao.exists(asset1.id))
        assertFalse(indexDao.exists("abc"))
    }

    @Test
    fun testGetAll() {
        val assets = indexDao.getAll(Pager.first(10))
        assertEquals(1, assets.list.size.toLong())
    }

    @Test
    fun testGetAllBySearchRequest() {
        val sb = SearchBuilder()
        sb.source.query(QueryBuilders.matchAllQuery())
        val assets = indexDao.getAll(Pager.first(10), sb)
        assertEquals(1, assets.list.size.toLong())
    }

    @Test
    @Throws(IOException::class)
    fun testGetAllBySearchRequestIntoStream() {
        indexDao.index(Source(getTestImagePath("set01/standard/faces.jpg")))
        refreshIndex()
        val stream = ByteArrayOutputStream()

        val sb = SearchBuilder()
        sb.source.query(QueryBuilders.matchAllQuery())
        sb.source.aggregation(AggregationBuilders.terms("path").field("source.path.raw"))

        indexDao.getAll(Pager.first(10), sb, stream)
        val result = Json.deserialize(stream.toString(), object : TypeReference<PagedList<Document>>() {})
        assertEquals(2, result.list.size.toLong())
        assertEquals(1, result.aggregations.entries.size)
    }

    @Test
    fun testGetAllScroll() {
        indexDao.index(Source(getTestImagePath("set01/standard/faces.jpg")))
        indexDao.index(Source(getTestImagePath("set01/standard/hyena.jpg")))
        indexDao.index(Source(getTestImagePath("set01/standard/toucan.jpg")))
        indexDao.index(Source(getTestImagePath("set01/standard/visa.jpg")))
        indexDao.index(Source(getTestImagePath("set01/standard/visa12.jpg")))
        refreshIndex()

        val sb = SearchBuilder()
        sb.source.query(QueryBuilders.matchAllQuery())
        sb.request.scroll("1m")

        var assets = indexDao.getAll(Pager.first(1), sb)
        assertEquals(1, assets.list.size.toLong())
        assertEquals(6, assets.page.totalCount as Long)
        assertNotNull(assets.scroll)
        val asset = assets.get(0)

        assets = indexDao.getAll(assets.scroll.id, "1m")
        assertEquals(1, assets.list.size.toLong())
        assertNotNull(assets.scroll)
        assertNotEquals(asset.id, assets.get(0).id)
    }

    @Test
    fun testBatchUpsert() {
        val source1 = Source(getTestImagePath("set04/standard/beer_kettle_01.jpg"))
        val source2 = Source(getTestImagePath("set04/standard/new_zealand_wellington_harbour.jpg"))

        var result = indexDao.index(ImmutableList.of(source1, source2))
        assertEquals(1, result.createdAssetIds.size)
        assertEquals(1, result.replacedAssetIds.size)

        result = indexDao.index(ImmutableList.of(source1, source2))
        assertEquals(0, result.createdAssetIds.size)
        assertEquals(2, result.replacedAssetIds.size)
    }

    @Test
    fun testAppendLink() {
        assertTrue(indexDao.appendLink("folder", "100",
                ImmutableList.of(asset1.id))["success"]!!.contains(asset1.id))
        assertTrue(indexDao.appendLink("parent", "foo",
                ImmutableList.of(asset1.id))["success"]!!.contains(asset1.id))

        val a = indexDao.get(asset1.id)
        val folder_links = a.getAttr<Collection<Any>>("system.links.folder")
        val parent_links = a.getAttr<Collection<Any>>("system.links.parent")

        assertEquals(1, folder_links!!.size.toLong())
        assertEquals(1, parent_links!!.size.toLong())
        assertTrue(folder_links.contains("100"))
        assertTrue(parent_links.contains("foo"))
    }

    @Test
    fun testRemoveLink() {
        assertTrue(indexDao.appendLink("folder", "100",
                ImmutableList.of(asset1.id))["success"]!!.contains(asset1.id))

        var a = indexDao.get(asset1.id)
        var links = a.getAttr<Collection<Any>>("system.links.folder")
        assertEquals(1, links!!.size.toLong())

        assertTrue(indexDao.removeLink("folder", "100",
                ImmutableList.of(asset1.id))["success"]!!.contains(asset1.id))

        a = indexDao.get(asset1.id)
        links = a.getAttr("system.links.folder")
        assertEquals(0, links!!.size.toLong())
    }

    @Test
    fun testUpdate() {
        asset1.setAttr("foo.bar", 100)
        indexDao.update(asset1)
        val asset2 = indexDao.get(asset1.id)
        assertEquals(100, (asset2.getAttr<Any>("foo.bar") as Int).toLong())
    }

    @Test
    fun testDelete() {
        assertTrue(indexDao.delete(asset1.id))
        refreshIndex()
        assertFalse(indexDao.delete(asset1.id))
    }

    @Test
    fun testBatchDelete() {
        val rsp1 = indexDao.batchDelete(listOf(asset1))
        refreshIndex()
        val rsp2 = indexDao.batchDelete(listOf(asset1))
        assertEquals(1, rsp1.deletedAssetIds.size)
        assertEquals(1, rsp1.totalRequested)
        assertEquals(0, rsp1.errors.size)

        assertEquals(0, rsp2.deletedAssetIds.size)
        assertEquals(1, rsp2.totalRequested)
        assertEquals(1, rsp2.missingAssetIds.size)
        assertEquals(0, rsp2.errors.size)
    }

    @Test
    @Throws(InterruptedException::class)
    fun testRetryBrokenFields() {

        val assets = ImmutableList.of<Document>(
                Source(getTestImagePath("set01/standard/faces.jpg")))
        assets[0].setAttr("custom.foobar", 1000)
        var result = indexDao.index(assets)
        refreshIndex()

        val next = ImmutableList.of<Document>(
                Source(getTestImagePath("set01/standard/hyena.jpg")),
                Source(getTestImagePath("set01/standard/toucan.jpg")),
                Source(getTestImagePath("set01/standard/visa.jpg")),
                Source(getTestImagePath("set01/standard/visa12.jpg")))
        for (s in next) {
            s.setAttr("custom.foobar", "bob")
        }
        result = indexDao.index(next)

        assertEquals(4, result.createdAssetIds.size)
        assertEquals(4, result.warningAssetIds.size)
        assertEquals(1, result.retryCount.toLong())
    }
}

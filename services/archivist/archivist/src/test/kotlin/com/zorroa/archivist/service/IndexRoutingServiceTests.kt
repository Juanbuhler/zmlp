package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.IndexRouteSpec
import com.zorroa.archivist.repository.IndexRouteDao
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.get.GetIndexRequest
import org.elasticsearch.client.RequestOptions
import org.junit.After
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IndexRoutingServiceTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    @Autowired
    lateinit var indexRouteDao: IndexRouteDao

    val testSpec = IndexRouteSpec("test", 1)

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @After
    fun reset() {
        RequestContextHolder.resetRequestAttributes()
    }

    @Test
    fun syncAllIndexRoutes() {
        jdbc.update(
            "UPDATE index_route SET str_mapping_type='test', int_mapping_major_ver=1, " +
                "int_mapping_minor_ver=0, str_index='test123'"
        )

        indexRoutingService.syncAllIndexRoutes()

        val route = indexRouteDao.getProjectRoute()
        assertEquals(route.minorVer, 20001231)
        assertTrue(indexRoutingService.getProjectRestClient().indexExists())
    }

    /**
     * Test the case where an index is deleted and has to be
     * fully synced with the latest patches.
     */
    @Test
    fun testSyncDeletedIndex() {
        val newRoute = indexRoutingService.createIndexRoute(testSpec)
        val rest = indexRoutingService.getProjectRestClient()
        val reqDel = DeleteIndexRequest(newRoute.indexName)

        try {
            rest.client.indices().delete(reqDel, RequestOptions.DEFAULT)
            logger.info("${newRoute.indexUrl} Elastic DB Removed")
        } catch (e: Exception) {
            logger.warn("Failed to delete 'unittest' index, this is usually ok.")
        }

        // The ES index is deleted but our route still shows an updated index
        val route = indexRouteDao.get(newRoute.id)
        assertTrue(route.minorVer > 0)

        val lastMapping = indexRoutingService.syncIndexRouteVersion(route)
        assertEquals(route.minorVer, lastMapping!!.minorVersion)
        assertTrue(indexRoutingService.getProjectRestClient().indexExists())
    }

    @Test
    fun syncIndexRouteVersion() {
        jdbc.update(
            "UPDATE index_route SET str_mapping_type='test', int_mapping_major_ver=1, " +
                "int_mapping_minor_ver=0, str_index='test123'"
        )

        var route = indexRouteDao.getProjectRoute()
        indexRoutingService.syncIndexRouteVersion(route)

        route = indexRouteDao.getProjectRoute()
        assertEquals(route.minorVer, 20001231)

        assertTrue(indexRoutingService.getProjectRestClient().indexExists())
    }

    @Test
    fun syncIndexRouteVersionShardsAndReplicas() {
        val index = "test123"
        jdbc.update(
            "UPDATE index_route SET str_mapping_type='test', int_mapping_major_ver=1, " +
                "int_mapping_minor_ver=0, str_index=?, int_shards=1, int_replicas=0", index
        )

        val rest = indexRoutingService.getProjectRestClient()
        try {
            val reqDel = DeleteIndexRequest(index)
            rest.client.indices().delete(reqDel, RequestOptions.DEFAULT)
        } catch (e: Exception) {
            // ignore
        }

        var route = indexRouteDao.getProjectRoute()
        indexRoutingService.syncIndexRouteVersion(route)

        // Validate the index has our settings.
        val getIndex = GetIndexRequest().indices("test123")
        val rsp = rest.client.indices().get(getIndex, RequestOptions.DEFAULT)
        assertEquals(1, rsp.settings["test123"].get("index.number_of_shards").toInt())
        assertEquals(0, rsp.settings["test123"].get("index.number_of_replicas").toInt())
    }

    @Test
    fun getMajorVersionMappingFile() {
        val mappingFile = indexRoutingService.getMajorVersionMappingFile("test", 1)
        assertEquals(1, mappingFile.majorVersion)
        assertEquals(0, mappingFile.minorVersion)
        assertEquals("test", mappingFile.name)
    }

    @Test
    fun getMinorVersionMappingFiles() {
        val files = indexRoutingService.getMinorVersionMappingFiles("test", 1)
        assertEquals(2, files.size)
        assertEquals(19991231, files[0].minorVersion)
        assertEquals(20001231, files[1].minorVersion)
    }

    @Test
    fun getOrgRestClientReIndex() {
        val route = indexRoutingService.createIndexRoute(testSpec)
        val request = MockHttpServletRequest()
        request.addHeader("X-Zorroa-Index-Route", route.id)
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
        indexRoutingService.getProjectRestClient()
    }

    @Test
    fun getClusterRestClient() {
        var route = indexRouteDao.getProjectRoute()
        val client = indexRoutingService.getClusterRestClient(route)
        assertTrue(client.indexExists())
        assertTrue(client.isAvailable())
    }

    @Test
    fun performHealthCheck() {
        val good = indexRoutingService.performHealthCheck()
        assertEquals("UP", good.status.code)

        jdbc.update("UPDATE index_cluster SET str_url='http://foo'")

        val bad = indexRoutingService.performHealthCheck()
        assertEquals("DOWN", bad.status.code)
    }

    @Test
    fun testGetIndexMappingVersions() {
        val mappings = indexRoutingService.getIndexMappingVersions()
        assertTrue(mappings.isNotEmpty())
    }

    @Test
    fun testCreate() {
        val route = indexRoutingService.createIndexRoute(testSpec)
        assertEquals(testSpec.mapping, route.mapping)
        assertEquals(testSpec.majorVer, route.majorVer)
    }

    @Test
    fun testCloseAndDelete() {
        val spec = IndexRouteSpec("english_strict", 1)
        val route = indexRoutingService.createIndexRoute(spec)
        indexRoutingService.closeAndDeleteIndex(route)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCreateMissingMapping() {
        val spec = IndexRouteSpec(
            "kirk",
            33
        )
        indexRoutingService.createIndexRoute(spec)
    }

    @Test
    fun testCloseIndex() {
        var route = indexRouteDao.getProjectRoute()
        assertTrue(indexRoutingService.closeIndex(route))

        val state = indexRoutingService.getEsIndexState(route)
        assertEquals("close", state["status"])
    }

    @Test
    fun testGetEsIndexState() {
        val route = indexRouteDao.getProjectRoute()
        val result = indexRoutingService.getEsIndexState(route)
        assertEquals("green", result["health"])
        assertEquals("open", result["status"])
        assertEquals("2", result["pri"])
        assertEquals("0", result["rep"])
    }

    @Test
    fun testDeleteIndex() {
        val spec = IndexRouteSpec("english_strict", 1)

        val route = indexRoutingService.createIndexRoute(spec)
        indexRoutingService.closeIndex(route)
        assertTrue(indexRoutingService.deleteIndex(route, force = true))
    }
}

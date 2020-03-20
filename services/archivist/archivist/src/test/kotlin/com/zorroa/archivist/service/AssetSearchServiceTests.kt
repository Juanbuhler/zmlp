package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AssetSpec
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.zmlp.util.Json
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AssetSearchServiceTests : AbstractTest() {

    @Autowired
    lateinit var assetSearchService: AssetSearchService

    @Before
    fun setUp() {
        val labels = listOf(
            mapOf("label" to "toucan", "score" to 0.5)
        )

        val spec = AssetSpec("https://i.imgur.com/LRoLTlK.jpg")
        val spec2 = AssetSpec("https://i.imgur.com/abc123442.jpg")

        val batchCreate = BatchCreateAssetsRequest(
            assets = listOf(spec, spec2)
        )
        val rsp = assetService.batchCreate(batchCreate)
        val id = rsp.created[0]
        val asset = assetService.getAsset(id)
        asset.setAttr("analysis.zvi.similarity.simhash", "AABBCC00")
        asset.setAttr("analysis.zvi.label-detection.labels", labels)
        assetService.index(id, asset.document, true)

        indexRoutingService.getProjectRestClient().refresh()
    }

    @Test
    fun testSearchSourceBuilderToMap() {
        val ssb = SearchSourceBuilder()
        ssb.query(QueryBuilders.termsQuery("bob", listOf("abc")))

        val map = assetSearchService.searchSourceBuilderToMap(ssb)
        val json = Json.Mapper.writeValueAsString(map)
        val expected = """{"query":{"terms":{"bob":["abc"],"boost":1.0}}}"""
        assertEquals(expected, json)
    }

    @Test
    fun testSearch() {
        val search = mapOf(
            "query" to mapOf("term" to mapOf("source.filename" to "LRoLTlK.jpg"))
        )

        val rsp = assetSearchService.search(search)
        assertEquals(1, rsp.hits.hits.size)
    }

    @Test
    fun testSearchWithSearchSourceBuilder() {
        val ssb = SearchSourceBuilder()
        ssb.query(QueryBuilders.matchAllQuery())

        val rsp = assetSearchService.search(ssb, mapOf())
        assertEquals(2, rsp.hits.hits.size)
    }

    @Test
    fun testCount() {
        val search = mapOf(
            "query" to mapOf("term" to mapOf("source.filename" to "LRoLTlK.jpg"))
        )

        val count = assetSearchService.count(search)
        assertEquals(1, count)
    }

    @Test
    fun testCountWithSearchSourceBuilder() {
        val ssb = SearchSourceBuilder()
        ssb.query(QueryBuilders.matchAllQuery())

        val count = assetSearchService.count(ssb)
        assertEquals(2, count)
    }

    @Test
    fun testScrollSearch() {
        val search = mapOf(
            "query" to mapOf("term" to mapOf("source.filename" to "LRoLTlK.jpg"))
        )

        val rsp = assetSearchService.search(search, mapOf("scroll" to arrayOf("1m")))
        assertEquals(1, rsp.hits.hits.size)
        assertNotNull(rsp.scrollId)
    }

    @Test
    fun testScroll() {
        val search = mapOf(
            "query" to mapOf("term" to mapOf("source.filename" to "LRoLTlK.jpg"))
        )

        val rsp = assetSearchService.search(search, mapOf("scroll" to arrayOf("5s")))
        assertNotNull(rsp.scrollId)

        val scroll = assetSearchService.scroll(mapOf(
            "scroll_id" to rsp.scrollId, "scroll" to "5s"))

        assertNotNull(scroll.scrollId)
        assertEquals(0, scroll.hits.hits.size)
    }

    @Test
    fun testClearScroll() {
        val search = mapOf(
            "query" to mapOf("term" to mapOf("source.filename" to "LRoLTlK.jpg"))
        )

        val rsp = assetSearchService.search(search, mapOf("scroll" to arrayOf("1m")))
        val result = assetSearchService.clearScroll(mapOf("scroll_id" to rsp.scrollId))
        assertTrue(result.isSucceeded)
    }

    @Test
    fun testSimilaritySearch() {
        val query = """{
            "query": {
                "function_score" : {
                    "functions" : [
                      {
                        "script_score" : {
                          "script" : {
                            "source" : "similarity",
                            "lang" : "zorroa-similarity",
                            "params" : {
                              "minScore" : 0.50,
                              "field" : "analysis.zvi.similarity.simhash",
                              "hashes" : ["AABBDD00"]
                            }
                          }
                        }
                      }
                    ],
                    "score_mode" : "multiply",
                    "boost_mode" : "replace",
                    "max_boost" : 3.4028235E38,
                    "min_score" : 0.50,
                    "boost" : 1.0
                  }
                }
            }
        """.trimIndent()
        val rsp = assetSearchService.search(Json.Mapper.readValue(query, Json.GENERIC_MAP))
        assertEquals(1, rsp.hits.hits.size)
        assertTrue(rsp.hits.hits[0].score > 0.98)
    }

    @Test
    fun testSimilaritySearch_noHits() {
        val query = """{
            "query": {
                "function_score" : {
                    "functions" : [
                      {
                        "script_score" : {
                          "script" : {
                            "source" : "similarity",
                            "lang" : "zorroa-similarity",
                            "params" : {
                              "minScore" : 0.50,
                              "field" : "analysis.zvi.similarity.simhash",
                              "hashes" : ["PPPPPPPP"]
                            }
                          }
                        }
                      }
                    ],
                    "score_mode" : "multiply",
                    "boost_mode" : "replace",
                    "max_boost" : 3.4028235E38,
                    "min_score" : 0.50,
                    "boost" : 1.0
                  }
                }
            }
        """.trimIndent()
        val rsp = assetSearchService.search(Json.Mapper.readValue(query, Json.GENERIC_MAP))
        assertEquals(0, rsp.hits.hits.size)
    }

    @Test
    fun testKwConfSearch() {
        val query = """{
            "query": {
                "function_score" : {
                    "functions" : [
                      {
                        "script_score" : {
                          "script" : {
                            "source" : "kwconf",
                            "lang" : "zorroa-kwconf",
                            "params" : {
                              "range": [0.5, 1.0],
                              "field" : "analysis.zvi.label-detection",
                              "labels" : ["toucan"]
                            }
                          }
                        }
                      }
                    ],
                    "score_mode" : "multiply",
                    "boost_mode" : "replace",
                    "max_boost" : 3.4028235E38,
                    "min_score" : 0.50,
                    "boost" : 1.0
                  }
                }
            }
        """.trimIndent()
        val rsp = assetSearchService.search(Json.Mapper.readValue(query, Json.GENERIC_MAP))
        assertEquals(1, rsp.hits.hits.size)
    }

    @Test
    fun testKwConfSearch_noHits() {
        val query = """{
            "query": {
                "function_score" : {
                    "functions" : [
                      {
                        "script_score" : {
                          "script" : {
                            "source" : "kwconf",
                            "lang" : "zorroa-kwconf",
                            "params" : {
                              "range": [0.1, 0.2],
                              "field" : "analysis.zvi.label-detection",
                              "labels" : ["toucan"]
                            }
                          }
                        }
                      }
                    ],
                    "score_mode" : "multiply",
                    "boost_mode" : "replace",
                    "max_boost" : 3.4028235E38,
                    "min_score" : 0.50,
                    "boost" : 1.0
                  }
                }
            }
        """.trimIndent()
        val rsp = assetSearchService.search(Json.Mapper.readValue(query, Json.GENERIC_MAP))
        assertEquals(0, rsp.hits.hits.size)
    }
}

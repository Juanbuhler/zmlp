package com.zorroa.irm.studio.repository

import com.zorroa.common.domain.Asset
import com.zorroa.common.domain.Document
import com.zorroa.common.util.Json
import com.zorroa.irm.studio.rest.EsRestClient
import com.zorroa.irm.studio.rest.EsRestClientCache
import org.elasticsearch.action.ActionListener
import org.elasticsearch.action.DocWriteRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.common.xcontent.XContentType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository
import java.lang.Exception

interface IndexDao {
    fun indexDocument(assetId: Asset, doc: Document)
}

@Repository
class ElasticSearchIndexDao @Autowired constructor(
        val esClientFactory: EsRestClientCache
): IndexDao {

    override fun indexDocument(assetId: Asset, doc: Document) {
        val esClient = esClientFactory.getClient(assetId.organizationId)
        if (doc.replace) {
            replaceDocument(esClient, doc)
        }
        else {
            upsertDocument(esClient, doc)
        }
    }

    private fun replaceDocument(esClient: EsRestClient, doc: Document) {
        val req = IndexRequest(esClient.org.indexName, doc.type, doc.id)
                .opType( DocWriteRequest.OpType.INDEX)
                .source(Json.serialize(doc.document), XContentType.JSON)
        if (esClient.org.routingKey != null) {
            req.routing(esClient.org.routingKey)
        }

        esClient.client.indexAsync(req, object: ActionListener<IndexResponse> {

            override fun onFailure(e: Exception?) {
                logger.warn("Failed to index asset: {}", doc.id, e)
            }

            override fun onResponse(rsp: IndexResponse?) {
                logger.info("indexed : {}", rsp?.id)
            }
        })
    }

    private fun upsertDocument(esClient: EsRestClient, doc: Document) {
        val req = UpdateRequest(esClient.org.indexName, doc.type, doc.id)
                .docAsUpsert(true)
                .doc(Json.serialize(doc.document), XContentType.JSON)
        if (esClient.org.routingKey != null) {
            req.routing(esClient.org.routingKey)
        }
        esClient.client.updateAsync(req, object: ActionListener<UpdateResponse> {
            override fun onFailure(e: Exception?) {
                logger.warn("Failed to update asset: {}", doc.id, e)
            }
            override fun onResponse(rsp: UpdateResponse?) {
                logger.info("indexed : {}", rsp?.id)
            }
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ElasticSearchIndexDao::class.java)
    }
}

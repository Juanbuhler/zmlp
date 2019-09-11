package com.zorroa.archivist.repository

import com.google.common.collect.Lists
import com.zorroa.archivist.domain.Access
import com.zorroa.archivist.domain.BatchDeleteAssetsResponse
import com.zorroa.archivist.domain.BatchIndexAssetsResponse
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.elastic.AbstractElasticDao
import com.zorroa.archivist.elastic.SearchHitRowMapper
import com.zorroa.archivist.elastic.SingleHit
import com.zorroa.archivist.security.AccessResolver
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getOrganizationFilter
import com.zorroa.archivist.service.MeterRegistryHolder.getTags
import com.zorroa.archivist.service.event
import com.zorroa.archivist.service.warnEvent
import com.zorroa.common.clients.SearchBuilder
import com.zorroa.common.domain.ArchivistSecurityException
import com.zorroa.common.util.Json
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.elasticsearch.action.DocWriteRequest
import org.elasticsearch.action.DocWriteResponse
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.index.IndexResponse
import org.elasticsearch.action.search.SearchType
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.fetch.subphase.FetchSourceContext
import org.springframework.dao.EmptyResultDataAccessException
import org.springframework.stereotype.Repository
import java.io.IOException
import java.io.OutputStream
import java.nio.file.Path
import java.util.regex.Pattern

interface IndexDao {

    fun getMapping(): Map<String, Any>

    fun delete(doc: Document): Boolean

    /**
     * Batch delete the given asset IDs.
     * @param ids the list of asset IDS to delete.
     */
    fun batchDelete(ids: List<Document>): BatchDeleteAssetsResponse

    fun get(id: String): Document

    fun getAll(ids: List<String>): List<Document>

    /**
     * Return the next page of an asset scroll.
     *
     * @param scrollId
     * @param timeout
     * @return
     */
    fun getAll(scrollId: String, timeout: String): PagedList<Document>

    /**
     * Get all assets given the page and SearchRequestBuilder.
     *
     * @param page
     * @param search
     * @return
     */
    fun getAll(page: Pager, search: SearchBuilder): PagedList<Document>

    fun getAll(page: Pager, search: SearchBuilder, stream: OutputStream)

    /**
     * Get all assets by page.
     *
     * @param page
     * @return
     */
    fun getAll(page: Pager): PagedList<Document>

    fun exists(id: String): Boolean

    fun get(path: Path): Document

    fun update(doc: Document): Long

    fun <T> getFieldValue(id: String, field: String): T?

    fun index(source: Document, refresh: Boolean = true): Document

    /**
     * Index the given sources.  If any assets are created, attach a source link.
     * @param sources
     * @return
     */
    fun index(sources: List<Document>): BatchIndexAssetsResponse

    fun index(sources: List<Document>, refresh: Boolean = true): BatchIndexAssetsResponse
}

@Repository
class IndexDaoImpl constructor(
    val meterRegistry: MeterRegistry,
    val accessResolver: AccessResolver
) : AbstractElasticDao(), IndexDao {

    override fun <T> getFieldValue(id: String, field: String): T? {
        val rest = getClient()
        val req = rest.newGetRequest(id)
            .fetchSourceContext(FetchSourceContext.FETCH_SOURCE)
        val d = Document(rest.client.get(req).source)
        // field values never have .raw since they come from source
        return d.getAttr(field.removeSuffix(".raw"))
    }

    override fun index(source: Document, refresh: Boolean): Document {
        index(listOf(source), refresh)
        return get(source.id)
    }

    override fun index(sources: List<Document>): BatchIndexAssetsResponse {
        return index(sources, true)
    }

    override fun index(sources: List<Document>, refresh: Boolean): BatchIndexAssetsResponse {
        val result = BatchIndexAssetsResponse(sources.size)
        if (sources.isEmpty()) {
            return result
        }

        val retries = Lists.newArrayList<Document>()
        val bulkRequest = BulkRequest()
        if (refresh) {
            bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE
        }

        for (source in sources) {
            bulkRequest.add(prepareInsert(source))
        }

        val rest = getClient()
        val bulk = rest.client.bulk(bulkRequest, RequestOptions.DEFAULT)

        var index = -1
        for (response in bulk.items) {
            index++
            if (response.isFailed) {
                /**
                 * If we hit this, then throw back to client.
                 */
                if ("cluster_block_exception" in response.failure.message) {
                    throw response.failure.cause
                }

                val message = response.failure.message
                val asset = sources[index]
                if (removeBrokenField(asset, message)) {
                    result.warningAssetIds.add(asset.id)
                    retries.add(sources[index])
                } else {
                    logger.warnEvent(
                        LogObject.ASSET, LogAction.BATCH_INDEX, message,
                        mapOf(
                            "assetId" to response.id,
                            "index" to response.index
                        )
                    )
                    result.erroredAssetIds.add(asset.id)
                }
            } else {
                when (response.opType) {
                    DocWriteRequest.OpType.INDEX -> {
                        val idxr = response.getResponse<IndexResponse>()
                        if (idxr.result == DocWriteResponse.Result.CREATED) {
                            result.createdAssetIds.add(idxr.id)
                        } else {
                            result.replacedAssetIds.add(idxr.id)
                        }
                    }
                }
            }
        }

        /*
         * TODO: limit number of retries to reasonable number.
         */
        if (!retries.isEmpty()) {
            result.retryCount++
            result.add(index(retries))
        }

        meterRegistry.counter(
            "zorroa.asset.index",
            getTags(Tag.of("status", "created"))
        )
            .increment(result.createdAssetIds.size.toDouble())

        meterRegistry.counter(
            "zorroa.asset.index",
            getTags(Tag.of("status", "replaced"))
        )
            .increment(result.replacedAssetIds.size.toDouble())

        meterRegistry.counter(
            "zorroa.asset.index",
            getTags(Tag.of("status", "rejected"))
        )
            .increment(result.erroredAssetIds.size.toDouble())

        meterRegistry.counter(
            "zorroa.asset.index",
            getTags(Tag.of("status", "warning"))
        )
            .increment(result.warningAssetIds.size.toDouble())

        return result
    }

    private fun prepareInsert(source: Document): IndexRequest {
        source.setAttr("system.organizationId", getOrgId().toString())
        val rest = getClient()
        return rest.newIndexRequest(source.id)
            .opType(DocWriteRequest.OpType.INDEX)
            .source(Json.serialize(source.document), XContentType.JSON)
    }

    private fun removeBrokenField(asset: Document, error: String): Boolean {
        for (pattern in RECOVERABLE_BULK_ERRORS) {
            val matcher = pattern.matcher(error)
            if (matcher.find()) {
                logger.warn(
                    "Removing broken field from {}: {}={}, {}", asset.id, matcher.group(1),
                    asset.getAttr(matcher.group(1)), error
                )
                return asset.removeAttr(matcher.group(1))
            }
        }
        return false
    }

    override fun update(asset: Document): Long {
        val rest = getClient()
        if (!accessResolver.hasAccess(Access.Write, asset)) {
            throw ArchivistSecurityException("Access denied")
        }
        val ver = rest.client.update(
            rest.newUpdateRequest(asset.id)
                .doc(Json.serializeToString(asset.document), XContentType.JSON)
                .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
        ).version
        return ver
    }

    override fun delete(doc: Document): Boolean {
        val rest = getClient()
        if (!accessResolver.hasAccess(Access.Delete, doc)) {
            throw ArchivistSecurityException("Access denied")
        }
        return rest.client.delete(rest.newDeleteRequest(doc.id)).result == DocWriteResponse.Result.DELETED
    }

    override fun batchDelete(docs: List<Document>): BatchDeleteAssetsResponse {
        if (docs.isEmpty()) {
            return BatchDeleteAssetsResponse()
        }

        val rsp = BatchDeleteAssetsResponse()
        val rest = getClient()
        val bulkRequest = BulkRequest()
            .setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)

        docs.forEach { doc ->
            val hold = doc.getAttr("system.hold", Boolean::class.java) ?: false
            if (doc.attrExists("system.hold") && hold) {
                rsp.onHoldAssetIds.add(doc.id)
            } else if (!accessResolver.hasAccess(Access.Delete, doc)) {
                rsp.accessDeniedAssetIds.add(doc.id)
            } else {
                rsp.totalRequested += 1
                bulkRequest.add(rest.newDeleteRequest(doc.id))
            }
        }

        if (rsp.totalRequested == 0) {
            return rsp
        }

        val bulk = rest.client.bulk(bulkRequest)
        for (br in bulk.items) {
            when {
                br.isFailed -> {
                    logger.warnEvent(
                        LogObject.ASSET, LogAction.BATCH_DELETE, br.failureMessage,
                        mapOf("assetId" to br.id, "index" to br.index)
                    )
                    rsp.errors[br.id] = br.failureMessage
                }
                else -> {
                    val deleted = br.getResponse<DeleteResponse>().result == DocWriteResponse.Result.DELETED
                    if (deleted) {
                        logger.event(
                            LogObject.ASSET,
                            LogAction.BATCH_DELETE,
                            mapOf("assetId" to br.id, "index" to br.index)
                        )
                        rsp.deletedAssetIds.add(br.id)
                    } else {
                        rsp.missingAssetIds.add(br.id)
                        logger.warnEvent(
                            LogObject.ASSET,
                            LogAction.BATCH_DELETE,
                            "Asset did not exist",
                            mapOf("assetId" to br.id, "index" to br.index)
                        )
                    }
                }
            }
        }

        meterRegistry.counter("zorroa.asset.delete", getTags(Tag.of("state", "success")))
            .increment(rsp.deletedAssetIds.size.toDouble())

        meterRegistry.counter("zorroa.asset.delete", getTags(Tag.of("state", "denied")))
            .increment(rsp.accessDeniedAssetIds.size.toDouble())

        meterRegistry.counter("zorroa.asset.delete", getTags(Tag.of("state", "error")))
            .increment(rsp.errors.size.toDouble())

        meterRegistry.counter("zorroa.asset.delete", getTags(Tag.of("state", "missing")))
            .increment(rsp.missingAssetIds.size.toDouble())

        meterRegistry.counter("zorroa.asset.delete", getTags(Tag.of("state", "on-hold")))
            .increment(rsp.onHoldAssetIds.size.toDouble())

        return rsp
    }

    override fun get(id: String): Document {
        val rest = getClient()
        val req = rest.newSearchBuilder()
        val query = QueryBuilders.boolQuery()
            .must(QueryBuilders.termQuery("_id", id))
            .filter(getOrganizationFilter())

        accessResolver.getAssetPermissionsFilter(Access.Read)?.let {
            query.filter(it)
        }
        req.source.size(1)
        req.source.query(query)

        return elastic.queryForObject(req, MAPPER)
    }

    override fun getAll(ids: List<String>): List<Document> {
        val rest = getClient()
        val req = rest.newSearchBuilder()
        val query = QueryBuilders.boolQuery()
            .must(QueryBuilders.termsQuery("_id", ids))
            .filter(getOrganizationFilter())
        accessResolver.getAssetPermissionsFilter(Access.Read)?.let {
            query.filter(it)
        }
        req.source.size(ids.size)
        req.source.query(query)

        return elastic.query(req, MAPPER)
    }

    override fun exists(id: String): Boolean {
        val rest = getClient()
        return rest.client.get(
            rest.newGetRequest(id)
                .fetchSourceContext(FetchSourceContext.DO_NOT_FETCH_SOURCE)
        ).isExists
    }

    override fun get(path: Path): Document {
        val rest = getClient()
        val req = rest.newSearchBuilder()
        val query = QueryBuilders.boolQuery()
            .must(QueryBuilders.termQuery("source.path.raw", path.toString()))
            .filter(getOrganizationFilter())
        accessResolver.getAssetPermissionsFilter(Access.Read)?.let {
            query.filter(it)
        }

        req.source.size(1)
        req.source.query(query)

        val assets = elastic.query(req, MAPPER)

        if (assets.isEmpty()) {
            throw EmptyResultDataAccessException("Asset $path does not exist", 1)
        }
        return assets[0]
    }

    override fun getAll(scrollId: String, timeout: String): PagedList<Document> {
        return elastic.scroll(scrollId, timeout, MAPPER)
    }

    override fun getAll(page: Pager, search: SearchBuilder): PagedList<Document> {
        return elastic.page(search, page, MAPPER)
    }

    @Throws(IOException::class)
    override fun getAll(page: Pager, search: SearchBuilder, stream: OutputStream) {
        elastic.page(search, page, stream)
    }

    override fun getAll(page: Pager): PagedList<Document> {
        val rest = getClient()
        val req = rest.newSearchBuilder()
        rest.routeSearchRequest(req.request)

        req.request.apply {
            searchType(SearchType.DFS_QUERY_THEN_FETCH)
        }
        req.source.apply {
            version(true)
            query(QueryBuilders.matchAllQuery())
        }

        return elastic.page(req, page, MAPPER)
    }

    override fun getMapping(): Map<String, Any> {
        return mapOf()
    }

    companion object {

        private val MAPPER = object : SearchHitRowMapper<Document> {
            override fun mapRow(hit: SingleHit): Document {
                val doc = Document()
                doc.document = hit.source
                doc.id = hit.id
                doc.score = hit.score
                doc.type = hit.type
                return doc
            }
        }

        private val RECOVERABLE_BULK_ERRORS = arrayOf(
            Pattern.compile("reason=failed to parse \\[(.*?)\\]"),
            Pattern.compile("\"term in field=\"(.*?)\"\""),
            Pattern.compile("mapper \\[(.*?)\\] of different type")
        )
    }
}

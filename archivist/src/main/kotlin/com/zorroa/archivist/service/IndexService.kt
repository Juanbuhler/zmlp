package com.zorroa.archivist.service

import com.google.common.collect.ImmutableList
import com.zorroa.archivist.config.ArchivistConfiguration
import com.zorroa.archivist.domain.Access
import com.zorroa.archivist.domain.AuditLogEntrySpec
import com.zorroa.archivist.domain.AuditLogType
import com.zorroa.archivist.domain.BatchDeleteAssetsResponse
import com.zorroa.archivist.domain.BatchIndexAssetsResponse
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.domain.PagedList
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.repository.AssetDao
import com.zorroa.archivist.repository.AuditLogDao
import com.zorroa.archivist.repository.IndexDao
import com.zorroa.archivist.search.AssetFilter
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.archivist.search.AssetSearchOrder
import com.zorroa.archivist.security.SecureRunnable
import com.zorroa.archivist.security.getAuthentication
import com.zorroa.archivist.security.hasPermission
import com.zorroa.archivist.security.withAuth
import com.zorroa.common.domain.ArchivistWriteException
import com.zorroa.common.schema.ProxySchema
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.core.task.TaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Component
import java.nio.file.Path
import java.nio.file.Paths
import javax.annotation.PostConstruct

/**
 * The IndexService is responsible for the business logic around asset CRUD and batch operations.
 */
interface IndexService {

    fun getMapping(): Map<String, Any>

    fun get(id: String): Document

    fun get(path: Path): Document

    fun getAll(ids: List<String>): List<Document>

    fun getProxies(id: String): ProxySchema

    fun getAll(page: Pager): PagedList<Document>

    fun index(assets: List<Document>): BatchIndexAssetsResponse

    fun index(doc: Document): Document

    fun exists(id: String): Boolean

    fun delete(assetId: String): Boolean

    fun batchDelete(assetId: List<String>): BatchDeleteAssetsResponse
}

@Component
class IndexServiceImpl @Autowired constructor(
    private val indexDao: IndexDao,
    private val assetDao: AssetDao,
    private val auditLogDao: AuditLogDao,
    private val fileServerProvider: FileServerProvider,
    private val fileStorageService: FileStorageService

) : IndexService {

    @Autowired
    lateinit var searchService: SearchService

    @Value("\${archivist.asset-store.sql-backup}")
    var assetStoreBackup: Boolean = false

    lateinit var workQueue: TaskExecutor

    @PostConstruct
    fun init() {
        workQueue = buildAssetWorkQueue()
    }

    override fun get(id: String): Document {
        return if (id.startsWith("/")) {
            get(Paths.get(id))
        } else {
            indexDao.get(id)
        }
    }

    override fun get(path: Path): Document {
        return indexDao.get(path)
    }

    override fun getAll(ids: List<String>): List<Document> {
        return indexDao.getAll(ids)
    }

    override fun getProxies(id: String): ProxySchema {
        val asset = get(id)
        val proxies = asset.getAttr("proxies", ProxySchema::class.java)

        if (proxies != null) {
            return proxies
        } else {

            for (hit in searchService.search(
                Pager.first(1), AssetSearch(
                    AssetFilter()
                        .addToTerms("media.clip.parent", id)
                )
                    .setFields(arrayOf("proxies"))
                    .setOrder(ImmutableList.of(AssetSearchOrder("_id")))
            )) {
                return hit.getAttr("proxies", ProxySchema::class.java) ?: ProxySchema()
            }

            return ProxySchema()
        }
    }

    override fun getAll(page: Pager): PagedList<Document> {
        return indexDao.getAll(page)
    }

    override fun index(doc: Document): Document {
        index(listOf(doc))
        return indexDao.get(doc.id)
    }

    override fun index(assets: List<Document>): BatchIndexAssetsResponse {
        val result = indexDao.index(assets, true)
        if (assetStoreBackup) {
            val auth = getAuthentication()
            workQueue.execute {
                withAuth(auth) {
                    assetDao.batchCreateOrReplace(assets)
                }
            }
        }
        return result
    }

    override fun exists(id: String): Boolean {
        return indexDao.exists(id)
    }

    /**
     * Batch delete the the given asset IDs, along with their supporting files.
     * This method propagates to children as well.
     *
     * @param assetIds: the IDs of the assets the delete.
     */
    override fun batchDelete(assetIds: List<String>): BatchDeleteAssetsResponse {
        if (assetIds.size > 1000) {
            throw ArchivistWriteException("Unable to delete more than 1000 assets in a single request")
        }

        if (assetIds.isEmpty()) {
            return BatchDeleteAssetsResponse()
        }

        /*
         * Setup an OR search where we target both the parents and children.
         */
        val search = AssetSearch()
        search.filter = AssetFilter()
        search.filter.should = listOf(
            AssetFilter().addToTerms("_id", assetIds).addToMissing("media.clip.parent"),
            AssetFilter().addToTerms("media.clip.parent", assetIds)
        )

        /*
         * Iterate a scan and scroll and batch delete each batch.
         * Along the way queue up work to delete any files.
         */
        val rsp = BatchDeleteAssetsResponse()
        searchService.scanAndScroll(search, true) { hits ->
            /*
             * Determine if any documents are on hold.
             */
            val docs = hits.hits.map { Document(it.id, it.sourceAsMap) }
            val batchRsp = indexDao.batchDelete(docs)

            auditLogDao.batchCreate(batchRsp.deletedAssetIds.map {
                AuditLogEntrySpec(it, AuditLogType.Deleted)
            })

            // add the batch results to the overall result.
            rsp.plus(batchRsp)

            workQueue.execute(SecureRunnable {
                docs.forEach {
                    if (it.id in batchRsp.deletedAssetIds) {
                        deleteAssociatedFiles(it)
                    }
                }
            })
        }
        return rsp
    }

    override fun delete(assetId: String): Boolean {
        val doc = indexDao.get(assetId)
        val result = indexDao.delete(doc)
        deleteAssociatedFiles(doc)
        if (result) {
            auditLogDao.create(AuditLogEntrySpec(assetId, AuditLogType.Deleted))
        }
        return result
    }

    fun deleteAssociatedFiles(doc: Document) {

        doc.getAttr("proxies", ProxySchema::class.java)?.let {
            it.proxies?.forEach { pr ->
                try {
                    val storage = fileStorageService.get(pr.id)
                    val ofile = fileServerProvider.getServableFile(storage.uri)
                    if (ofile.delete()) {
                        logger.event(
                            LogObject.STORAGE, LogAction.DELETE,
                            mapOf("proxyId" to pr.id, "assetId" to doc.id)
                        )
                    } else {
                        logger.warnEvent(
                            LogObject.STORAGE, LogAction.DELETE, "file did not exist",
                            mapOf("proxyId" to pr.id)
                        )
                    }
                } catch (e: Exception) {
                    logger.warnEvent(
                        LogObject.STORAGE, LogAction.DELETE, e.message ?: e.javaClass.name,
                        mapOf("proxyId" to pr.id), e
                    )
                }
            }
        }
    }

    private fun buildAssetWorkQueue(): TaskExecutor {
        return if (ArchivistConfiguration.unittest) {
            SyncTaskExecutor()
        } else {
            val tpe = ThreadPoolTaskExecutor()
            tpe.corePoolSize = 4
            tpe.maxPoolSize = 4
            tpe.threadNamePrefix = "ASSET-QUEUE-"
            tpe.isDaemon = true
            tpe.setQueueCapacity(1000)
            tpe.initialize()
            return tpe
        }
    }

    override fun getMapping(): Map<String, Any> {
        return indexDao.getMapping()
    }

    companion object {

        private val logger = LoggerFactory.getLogger(IndexServiceImpl::class.java)
    }
}

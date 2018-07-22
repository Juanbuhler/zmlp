package com.zorroa.analyst.service

import com.zorroa.analyst.domain.PreconditionFailedException
import com.zorroa.analyst.domain.UpdateStatus
import com.zorroa.analyst.repository.IndexDao
import com.zorroa.common.domain.Asset
import com.zorroa.common.domain.Document
import com.zorroa.common.service.CoreDataVaultService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*


interface AssetService {
    fun getDocument(assetId: Asset) : Document
    fun storeAndReindex(assetId: Asset, doc: Document) : UpdateStatus
    fun getAsset(doc: Document) : Asset
}

@Service
class AssetServiceImpl @Autowired constructor(
        val coreDataVault: CoreDataVaultService,
        val indexDao: IndexDao): AssetService {

    /**
     * A fakeish implementation for now.
     */
    override fun getAsset(doc: Document) : Asset {

        if (!doc.attrExists("zorroa.organizationId")) {
            throw PreconditionFailedException("Asset ${doc.id} has no organization Id")
        }

        return Asset(UUID.fromString(doc.id),
                UUID.fromString(doc.getAttr("zorroa.organizationId")))
    }

    override fun getDocument(assetId: Asset): Document {
        return coreDataVault.getIndexedMetadata(assetId)
    }

    override fun storeAndReindex(assetId: Asset, doc: Document) : UpdateStatus {
        val result = UpdateStatus()
        try {
            coreDataVault.updateIndexedMetadata(assetId, doc)
            result.status["stored"] = true
            logger.info("Stored: {}", assetId.id)
        } catch (e: Exception) {
            logger.warn("Failed to write {} into CDV", assetId.id,e)
        }
        try {
            indexDao.indexDocument(assetId, doc)
            result.status["indexed"] = true
            logger.info("Indexed: {}", assetId.id)
        } catch(e: Exception) {
            logger.warn("Failed to write {} into Index", assetId.id,e)
        }

        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AssetServiceImpl::class.java)
    }
}

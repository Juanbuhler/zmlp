package com.zorroa.archivist.client

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Document
import com.zorroa.common.clients.CoreDataVaultAssetSpec
import com.zorroa.common.clients.IrmCoreDataVaultClientImpl
import com.zorroa.common.util.Json
import io.micrometer.core.instrument.MeterRegistry
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.nio.file.Paths
import java.util.*
import kotlin.test.*


/**
 * Unless you are configured with the proper service-credentials.json file, these tests
 * will not pass.
 */
@Ignore
class CoreDataVaultClientTests : AbstractTest() {

    val companyId = 25274

    val docType = "01059ec7-42e8-48f3-adcf-4fa726005ab4"

    @Autowired
    lateinit var meterRegistry : MeterRegistry

    lateinit var client :  IrmCoreDataVaultClientImpl

    @Before
    fun init() {
        client = IrmCoreDataVaultClientImpl("https://cdvapi.dit3-insight.com",
                Paths.get("unittest/config/service-credentials.json"),
                Paths.get("unittest/config/data-credentials.json"),
                meterRegistry)
    }

    @Test
    fun testAssetExists() {
        val id = UUID.randomUUID().toString()
        val spec = CoreDataVaultAssetSpec(id, docType,"test.pdf")
        val asset1 = client.createAsset(companyId, spec)
        assertTrue(client.assetExists(companyId, asset1["documentGUID"] as String))
        assertFalse(client.assetExists(companyId, "705EF325-E6E4-4DA7-AD26-C610C70261A8"))
    }

    @Test
    fun testGetDocumentTypes() {
        val res = client.getDocumentTypes(companyId)
        assertTrue(res.isNotEmpty())
        assertTrue(res[0].containsKey("documentTypeId"))
    }

    @Test
    fun testGetAsset() {
        val id = UUID.randomUUID().toString()
        val spec = CoreDataVaultAssetSpec(id, docType,"test.pdf")
        val asset1 = client.createAsset(companyId, spec)
        val asset2 = client.getAsset(companyId, asset1["documentGUID"] as String)

        assertEquals(spec.fileName, asset2["fileName"])
        assertEquals(spec.documentTypeId, asset2["documentTypeId"])
    }

    @Test
    fun testCreateAsset() {
        val id = UUID.randomUUID().toString()
        val spec = CoreDataVaultAssetSpec(id, docType,"test.pdf")
        val asset1 = client.createAsset(companyId, spec)

        println(Json.prettyString(asset1))
        assertEquals(spec.fileName, asset1["fileName"])
        assertEquals(spec.documentTypeId, asset1["documentTypeId"])
    }

    @Test
    fun testUpdateAsset() {
        val id = UUID.randomUUID()
        val asset1 = client.createAsset(companyId,
                CoreDataVaultAssetSpec(id, docType, "test.pdf")).toMutableMap()
        asset1["fileName"] = "bob.pdf"
        val result = client.updateAsset(companyId, asset1)
        assertEquals("bob.pdf", result["fileName"])
    }

    @Test(expected = com.zorroa.common.clients.RestClientException::class)
    fun testUpdateAssetDoesNotExist() {
        client.updateAsset(companyId,
                mapOf("documentGUID" to "077F5AB7-614F-4E76-AF4A-B37DA23DCB9E",
                        "fileName" to "bob.jpg"))
    }

    @Test
    fun testGetIndexedMetadata() {
        val id = UUID.randomUUID()
        val asset1 = client.createAsset(companyId,
                CoreDataVaultAssetSpec(id, docType, "test.pdf")).toMutableMap()
        val doc = Document(asset1["documentGUID"] as String)
        doc.setAttr("foo", "bar")
        assertTrue(client.updateIndexedMetadata(companyId, doc))
        val doc2 = client.getIndexedMetadata(companyId, doc.id)
        assertEquals(doc.id, doc2.id)
        assertEquals(doc.getAttr("foo", String::class.java), doc2.getAttr("foo", String::class.java))
    }

    @Test
    fun testGetMissingIndexedMetadataEmpty() {
        val id = UUID.randomUUID().toString()
        val doc = client.getIndexedMetadata(companyId, id)
        assertEquals(id, doc.id)
        assertTrue(doc.document.isEmpty())
    }

    @Test
    fun testUpdateIndexedMetadata() {
        val id = UUID.randomUUID()
        val asset1 = client.createAsset(companyId,
                CoreDataVaultAssetSpec(id, docType, "test.pdf")).toMutableMap()
        val doc = Document(asset1["documentGUID"] as String)
        doc.setAttr("foo", "bar")
        assertTrue(client.updateIndexedMetadata(companyId, doc))
    }

    @Test
    fun testUpdateESMetadataDoesNotExist() {
        val doc = Document("077F5AB7-614F-4E76-AF4A-B37DA23DCB9E")
        val res = client.updateIndexedMetadata(companyId, doc)
        assertFalse(res)
    }

    @Test
    fun testBatchUpdateIndexedMetdata() {
        val id = UUID.randomUUID()
        val asset1 = client.createAsset(companyId,
                CoreDataVaultAssetSpec(id, docType, "test.pdf")).toMutableMap()

        val doc = Document(asset1["documentGUID"] as String)
        doc.setAttr("foo", "bar")

        val result = client.batchUpdateIndexedMetadata(companyId, listOf(doc))
        assertTrue(result[doc.id] ?: false)
    }

    @Test(expected=com.zorroa.common.clients.RestClientException::class)
    fun testDelete() {
        val id = UUID.randomUUID()
        val asset1 = client.createAsset(companyId,
                CoreDataVaultAssetSpec(id, docType, "test.pdf")).toMutableMap()
        val assetId =  asset1["documentGUID"] as String
        assertTrue(client.delete(companyId, assetId))
        client.getAsset(companyId, assetId)
    }

    @Test
    fun testBatchDelete() {
        val id = UUID.randomUUID()
        val asset1 = client.createAsset(companyId,
                CoreDataVaultAssetSpec(id, docType, "test.pdf")).toMutableMap()
        val assetId =  asset1["documentGUID"] as String
        val result = client.batchDelete(companyId, listOf(assetId))
        assertTrue(result[assetId] ?: false)
    }
}
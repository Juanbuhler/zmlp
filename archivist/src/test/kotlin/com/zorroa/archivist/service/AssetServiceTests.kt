package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.Acl
import com.zorroa.archivist.domain.AuditLogFilter
import com.zorroa.archivist.domain.AuditLogType
import com.zorroa.archivist.domain.BatchCreateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdateAssetLinks
import com.zorroa.archivist.domain.BatchUpdateAssetsRequest
import com.zorroa.archivist.domain.BatchUpdatePermissionsRequest
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.domain.FieldEditSpec
import com.zorroa.archivist.domain.LinkType
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.domain.UpdateAssetRequest
import com.zorroa.archivist.repository.AuditLogDao
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.common.schema.PermissionSchema
import com.zorroa.common.util.Json
import com.zorroa.security.Groups
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AssetServiceTests : AbstractTest() {

    @Autowired
    lateinit var auditLogDao: AuditLogDao

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    override fun requiresFieldSets(): Boolean {
        return true
    }

    @Before
    fun init() {
        addTestAssets("set04/standard")
    }

    fun testBatchCreateOrReplace() {
        var assets = searchService.search(Pager.first(), AssetSearch())
        for (asset in assets) {
            validateSystemAttrsExist(asset)
            assertEquals(asset.getAttr("system.timeCreated",
                    String::class.java),
                    asset.getAttr("system.timeModified", String::class.java))
        }

        Thread.sleep(1000)
        val rsp = assetService.createOrReplaceAssets(BatchCreateAssetsRequest(assets.list))
        assertEquals(2, rsp.replacedAssetIds.size)
        assertEquals(0, rsp.createdAssetIds.size)

        assets = searchService.search(Pager.first(), AssetSearch())
        for (asset in assets) {
            validateSystemAttrsExist(asset)
            assertNotEquals(asset.getAttr("system.timeCreated",
                    String::class.java),
                    asset.getAttr("system.timeModified", String::class.java))
        }
    }

    @Test
    fun testBatchCreateOrReplaceWithLinks() {
        var assets = searchService.search(Pager.first(), AssetSearch())
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(assets.list))

        val ids = assets.map { it.id }
        val folderId = UUID.randomUUID()
        assertEquals(2, assetService.addLinks(
                LinkType.Folder, folderId, BatchUpdateAssetLinks(ids)).updatedAssetIds.size)

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(assets.list))
        assets = searchService.search(Pager.first(), AssetSearch())
        assertTrue(assets.size() > 0)
        assets.list.forEach {
            assertTrue(it.attrExists("system.links"))
        }
    }

    @Test
    fun testBatchCreateOrReplaceWithManualEdits() {

        val field = fieldSystemService.getField("media.title")

        var assets = searchService.search(Pager.first(), AssetSearch())
        assets.list.forEach {
            assetService.createFieldEdit(FieldEditSpec(UUID.fromString(it.id), field.id, null, "bilbo"))
        }

        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(assets.list))
        assets = searchService.search(Pager.first(), AssetSearch())
        assertTrue(assets.size() > 0)
        assets.list.forEach {
            assertEquals("bilbo", it.getAttr("media.title", String::class.java))
        }
    }

    @Test
    fun testCreateOrReplace() {
        val asset = searchService.search(Pager.first(), AssetSearch())[0]
        asset.setAttr("foo", "bar")
        assetService.createOrReplaceAssets(BatchCreateAssetsRequest(asset))
        val newAsset = assetService.get(asset.id)
        assertEquals(newAsset.getAttr("foo", String::class.java), "bar")
    }

    @Test
    fun testGet() {
        val asset1 = searchService.search(Pager.first(), AssetSearch())[0]
        val asset2 = assetService.get(asset1.id)
        assertEquals(asset1.id, asset2.id)
        assertEquals(asset1.getAttr("source.path", String::class.java),
                asset2.getAttr("source.path", String::class.java))
    }

    @Test
    fun testBatchUpdate() {

        var updates = mutableMapOf<String, UpdateAssetRequest>()
        searchService.search(Pager.first(), AssetSearch()).forEach { doc ->
            updates[doc.id] = UpdateAssetRequest(mapOf("foos" to "ball"))
        }
        val rsp = assetService.updateAssets(BatchUpdateAssetsRequest(updates))
        assertEquals(2, rsp.updatedAssetIds.size)

        val search = searchService.search(Pager.first(), AssetSearch())
        assertEquals(2, search.size())
        for (doc: Document in search.list) {
            assertEquals("ball", doc.getAttr("foos", String::class.java))
        }
    }

    @Test
    fun testBatchUpdateCheckSuggestions() {

        addTestAssets("set04/standard")

        val word = "mr.stubbins"
        var updates = mutableMapOf<String, UpdateAssetRequest>()
        searchService.search(Pager.first(), AssetSearch()).forEach { doc ->
            updates[doc.id] = UpdateAssetRequest(mapOf("media.title" to word))
        }
        val rsp = assetService.updateAssets(BatchUpdateAssetsRequest(updates))
        assertEquals(2, rsp.updatedAssetIds.size)

        val search = searchService.search(Pager.first(), AssetSearch())
        assertEquals(2, search.size())
        for (doc: Document in search.list) {
            val suggest = doc.getAttr("system.suggestions", Json.LIST_OF_STRINGS)
            assertTrue(suggest.contains(word))
        }
    }

    @Test
    fun testBatchUpdateRemove() {
        val batch = mutableMapOf<String, Map<String, Any?>>()

        var updates = mutableMapOf<String, UpdateAssetRequest>()
        searchService.search(Pager.first(), AssetSearch()).forEach { doc ->
            updates[doc.id] = UpdateAssetRequest(null, listOf("source"))
        }
        val rsp = assetService.updateAssets(BatchUpdateAssetsRequest(updates))
        assertEquals(2, rsp.updatedAssetIds.size)

        val search = searchService.search(Pager.first(), AssetSearch())
        assertEquals(2, search.size())
        for (doc: Document in search.list) {
            assertNull(doc.getAttr("source"))
        }
    }

    @Test
    fun testDelete() {

        val page = searchService.search(Pager.first(), AssetSearch())
        assertTrue(assetService.delete(page[0].id))
        refreshIndex()

        val leftOvers = searchService.search(Pager.first(), AssetSearch())
        assertEquals(page.size() - leftOvers.size(), 1)
    }

    @Test
    fun testBatchDelete() {
        val page = searchService.search(Pager.first(), AssetSearch())
        val ids = page.map { it.id }
        val rsp = assetService.batchDelete(ids)
        assertEquals(0, rsp.errors.size)
        assertEquals(2, rsp.deletedAssetIds.size)
        assertEquals(2, rsp.totalRequested)
    }

    @Test
    fun testAddLinks() {
        val page = searchService.search(Pager.first(), AssetSearch())
        val ids = page.map { it.id }
        val folderId = UUID.randomUUID()
        assertEquals(2, assetService.addLinks(
                LinkType.Folder, folderId, BatchUpdateAssetLinks(ids)).updatedAssetIds.size)
        assertEquals(0, assetService.addLinks(
                LinkType.Folder, folderId, BatchUpdateAssetLinks(ids)).erroredAssetIds.size)
    }

    @Test
    fun testAddChildLinks() {
        val page = searchService.search(Pager.first(), AssetSearch())
        val batch = BatchUpdateAssetsRequest(
                mapOf(page[0].id to UpdateAssetRequest(mapOf("media.clip.parent" to page[1].id))))
        assetService.updateAssets(batch)

        val folderId = UUID.randomUUID()
        assertEquals(1,
                assetService.addLinks(LinkType.Folder, folderId,
                        BatchUpdateAssetLinks(null, listOf(page[1].id), AssetSearch())).updatedAssetIds.size)
    }

    @Test
    fun testRemoveLinks() {
        val page = searchService.search(Pager.first(), AssetSearch())
        val ids = page.map { it.id }
        val folderId = UUID.randomUUID()
        assertEquals(2, assetService.addLinks(
                LinkType.Folder, folderId, BatchUpdateAssetLinks(ids)).updatedAssetIds.size)
        assertEquals(2, assetService.removeLinks(LinkType.Folder, folderId, ids).updatedAssetIds.size)
        assertEquals(0, assetService.removeLinks(LinkType.Folder, folderId, ids).updatedAssetIds.size)
    }

    @Test
    fun testSetPermissions() {
        val perm = permissionService.getPermission(Groups.ADMIN)
        assetService.setPermissions(
                BatchUpdatePermissionsRequest(AssetSearch(), Acl().addEntry(perm.id, 4)))

        val page = searchService.search(Pager.first(), AssetSearch())
        for (asset in page) {
            val perms = asset.getAttr("system.permissions", PermissionSchema::class.java)
            assertTrue(perms!!.export.contains(perm.id))
        }
    }

    @Test
    fun testSetPermissionsWithReplace() {
        val perm = permissionService.getPermission(Groups.ADMIN)
        assetService.setPermissions(
            BatchUpdatePermissionsRequest(AssetSearch(), Acl().addEntry(perm.id, 4), replace = true))

        val page = searchService.search(Pager.first(), AssetSearch())
        for (asset in page) {
            val perms = asset.getAttr("system.permissions", PermissionSchema::class.java)
            assertTrue(perms!!.export.contains(perm.id))
            assertTrue(perms!!.read.isEmpty())
            assertTrue(perms!!.write.isEmpty())
        }
    }

    @Test
    fun testCreateFieldEdit() {
        val title = "khaaaaaan"
        val field = fieldSystemService.getField("media.title")

        val page = searchService.search(Pager.first(), AssetSearch())
        for (asset in page) {
            assetService.createFieldEdit(
                    FieldEditSpec(UUID.fromString(asset.id), field.id, null, title))
        }
        for (asset in searchService.search(Pager.first(), AssetSearch()).list) {
            assertEquals(title, asset.getAttr("media.title", String::class.java))
        }

        val result =
                auditLogDao.getAll(AuditLogFilter(
                        fieldIds = listOf(field.id),
                        types = listOf(AuditLogType.Changed)))
        assertTrue(result.size() > 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCreateFieldEditRequireList() {
        jdbc.update("UPDATE field SET bool_list='t' WHERE str_attr_name='media.title'")

        val title = "khaaaaaan"
        val field = fieldSystemService.getField("media.title")
        assertTrue(field.requireList)

        val page = searchService.search(Pager.first(), AssetSearch())
        val asset = page.list.first()

        assetService.createFieldEdit(
            FieldEditSpec(UUID.fromString(asset.id), field.id, null, title))
    }

    @Test
    fun testDeleteFieldEdit() {
        val title = "khaaaaaan"
        val field = fieldSystemService.getField("media.title")

        var page = searchService.search(Pager.first(), AssetSearch())
        var asset = page.list[0]
        val edit = assetService.createFieldEdit(
                FieldEditSpec(UUID.fromString(asset.id), field.id, null, title))
        assertTrue(assetService.deleteFieldEdit(edit))

        page = searchService.search(Pager.first(), AssetSearch())
        for (asset in page) {
            assertNotEquals(title, asset.getAttr("media.title", String::class.java))
        }

        val result =
                auditLogDao.getAll(AuditLogFilter(
                        fieldIds = listOf(field.id),
                        types = listOf(AuditLogType.Changed)))

        assertTrue(result[0].message!!.contains("undo"))
        assertTrue(result[1].message!!.contains("manual edit"))
    }

    /**
     * Just checks we didn't lose any system attributes after
     * reprocessing, does not validate any values.
     */
    fun validateSystemAttrsExist(asset: Document) {
        assertNotNull(asset.getAttr("system.permissions"))
        assertNotNull(asset.getAttr("system.timeCreated"))
        assertNotNull(asset.getAttr("system.timeModified"))
        assertNotNull(asset.getAttr("system.links"))
    }
}

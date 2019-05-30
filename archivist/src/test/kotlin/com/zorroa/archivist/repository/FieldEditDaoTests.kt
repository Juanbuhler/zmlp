package com.zorroa.archivist.repository

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AttrType
import com.zorroa.archivist.domain.Field
import com.zorroa.archivist.domain.FieldEditSpecInternal
import com.zorroa.archivist.domain.FieldSpecExpose
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals

class FieldEditDaoTests : AbstractTest() {

    @Autowired
    lateinit var fieldEditDao: FieldEditDao

    lateinit var field: Field

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()
        field = fieldSystemService.createField(FieldSpecExpose(
                "File Extension", "source.extension", AttrType.StringAnalyzed)
        )
    }

    @Test
    fun testCreate() {
        val spec = FieldEditSpecInternal(UUID.randomUUID(), field.id, "pig", "jpg")
        val edit = fieldEditDao.create(spec)
        assertEquals("pig", edit.newValue)
        assertEquals("jpg", edit.oldValue)
    }

    @Test
    fun testCreateDuplicate() {
        val assetId = UUID.randomUUID()
        val spec = FieldEditSpecInternal(assetId, field.id, "pig", "jpg")

        val edit1 = fieldEditDao.create(spec)
        val edit2 = fieldEditDao.create(spec)
        assertEquals(edit1.id, edit2.id)
    }

    @Test
    fun testGetAssetUpdateMap() {
        val spec = FieldEditSpecInternal(UUID.randomUUID(), field.id, "pig", "jpg")
        val edit = fieldEditDao.create(spec)

        val map = fieldEditDao.getAssetUpdateMap(spec.assetId)
        assertEquals("pig", map[field.attrName])
    }
}

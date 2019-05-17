package com.zorroa.archivist.service

import com.zorroa.archivist.AbstractTest
import com.zorroa.archivist.domain.AttrType
import com.zorroa.archivist.domain.FieldEditSpec
import com.zorroa.archivist.domain.FieldSpec
import com.zorroa.archivist.domain.FieldUpdateSpec
import com.zorroa.archivist.domain.Pager
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.common.domain.JobFilter
import com.zorroa.common.util.Json
import org.junit.Before
import org.junit.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FieldSystemServiceTests : AbstractTest() {

    @Autowired
    lateinit var jobService: JobService

    override fun requiresElasticSearch(): Boolean {
        return true
    }

    @Before
    fun init() {
        addTestAssets("set04/standard")
        refreshIndex()
    }

    @Test
    fun createRegular() {
        val spec = FieldSpec("File Extension", "source.extension", null, false)
        val field = fieldSystemService.createField(spec)
        assertEquals("source.extension", field.attrName)
        assertEquals("File Extension", field.name)
        assertEquals(field.attrType, AttrType.StringAnalyzed)
        assertEquals(false, field.editable)
    }

    @Test
    fun createCustomStringContentField() {
        val spec = FieldSpec("Notes", null, AttrType.StringContent, true)
        val field = fieldSystemService.createField(spec)
        assertEquals(AttrType.StringContent, field.attrType)
        assertTrue(field.custom)
        assertEquals("custom.string_content__0", field.attrName)
        assertEquals("Notes", field.name)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, "ABC"))
        assertEquals(AttrType.StringContent, fieldSystemService.getEsAttrType("custom.string_content__0"))
    }

    @Test
    fun createCustomStringExactField() {
        val spec = FieldSpec("SomeField", null, AttrType.StringExact, true)
        val field = fieldSystemService.createField(spec)
        assertEquals(AttrType.StringExact, field.attrType)
        assertTrue(field.custom)
        assertEquals("custom.string_exact__0", field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, "ABC"))
        assertEquals(AttrType.StringExact, fieldSystemService.getEsAttrType("custom.string_exact__0"))
    }

    @Test
    fun createCustomStringAnalyzedField() {
        val attrName = "custom.string_analyzed__0"
        val attrType = AttrType.StringAnalyzed

        val spec = FieldSpec("SomeField", null, attrType, true)
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, "ABC"))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomStringPathField() {
        val attrName = "custom.string_path__0"
        val attrType = AttrType.StringPath

        val spec = FieldSpec("SomeField", null, attrType, true)
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, "/ABC/123"))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomNumberIntegerField() {
        val attrName = "custom.number_integer__0"
        val attrType = AttrType.NumberInteger

        val spec = FieldSpec("SomeField", null, attrType, true)
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, 2112))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomNumberFloatField() {
        val attrName = "custom.number_float__0"
        val attrType = AttrType.NumberFloat

        val spec = FieldSpec("SomeField", null, attrType, true)
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, 2.22))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomBooleanField() {
        val attrName = "custom.boolean__0"
        val attrType = AttrType.Bool

        val spec = FieldSpec("SomeField", null, attrType, true)
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, true))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomField() {
        val attrName = "custom.boolean__0"
        val attrType = AttrType.Bool

        val spec = FieldSpec("SomeField", null, attrType, true)
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, true))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomDateTimeFieldAsLong() {
        val attrName = "custom.date_time__0"
        val attrType = AttrType.DateTime

        val spec = FieldSpec("SomeField", null, attrType, true)
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, System.currentTimeMillis()))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun createCustomDateTimeFieldAsString() {
        val attrName = "custom.date_time__0"
        val attrType = AttrType.DateTime

        val spec = FieldSpec("SomeField", null, attrType, true)
        val field = fieldSystemService.createField(spec)
        assertEquals(attrType, field.attrType)
        assertTrue(field.custom)
        assertEquals(attrName, field.attrName)

        val date = "11/12/1974 10:14:52"
        println(date)
        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, date))
        assertEquals(attrType, fieldSystemService.getEsAttrType(attrName))
    }

    @Test
    fun applyFieldEdits() {
        val asset = searchService.search(Pager.first(), AssetSearch()).list.first()
        val field = fieldSystemService.getField("media.title")
        val edit = assetService.createFieldEdit(FieldEditSpec(asset.id, field.id, null, "bilbo"))

        fieldSystemService.applyFieldEdits(asset)
        assertEquals("bilbo", asset.getAttr("media.title", String::class.java))
    }

    @Test
    fun getEsTypeMap() {
        val typeMap = fieldSystemService.getEsTypeMap()
        assertEquals(AttrType.StringAnalyzed, typeMap["source.filename"])
        assertEquals(AttrType.StringExact, typeMap["media.clip.parent"])
        assertEquals(AttrType.NumberFloat, typeMap["media.clip.length"])
        assertEquals(AttrType.NumberInteger, typeMap["media.width"])
        assertEquals(AttrType.GeoPoint, typeMap["location.point"])
        assertEquals(AttrType.DateTime, typeMap["system.timeModified"])
    }

    @Test
    fun getEsAttrType() {
        assertEquals(AttrType.StringAnalyzed, fieldSystemService.getEsAttrType("source.filename"))
        assertEquals(AttrType.StringExact, fieldSystemService.getEsAttrType("media.clip.parent"))
        assertEquals(AttrType.NumberFloat, fieldSystemService.getEsAttrType("media.clip.length"))
        assertEquals(AttrType.NumberInteger, fieldSystemService.getEsAttrType("media.width"))
        assertEquals(AttrType.GeoPoint, fieldSystemService.getEsAttrType("location.point"))
        assertEquals(AttrType.DateTime, fieldSystemService.getEsAttrType("system.timeModified"))
    }

    @Test
    fun getEsMapping() {
        // Not testing this 2 hard, the getEsAttrType and  getEsTypeMap are testing it more in depth.
        val mapping = fieldSystemService.getEsMapping()
        assertTrue("unittest" in mapping)
    }

    @Test
    fun testCreateSuggestField() {
        val jobCount = jobService.getAll(JobFilter()).size()
        val spec = FieldSpec("File Extension", "foo.bar", AttrType.StringAnalyzed,
                keywords = true, suggest = true)
        val field = fieldSystemService.createField(spec)
        assertEquals(true, field.suggest)
        assertEquals(true, field.keywords)
        assertEquals(jobCount + 1, jobService.getAll(JobFilter()).size(),
                "reindex job was not created")
    }

    @Test
    fun testCreateSuggestFieldSkipReindex() {
        val jobCount = jobService.getAll(JobFilter()).size()
        val spec = FieldSpec("File Extension", "foo.bar", AttrType.StringAnalyzed,
                keywords = true, suggest = true)
        fieldSystemService.createField(spec, reindexSuggest = false)
        assertEquals(jobCount, jobService.getAll(JobFilter()).size(),
                "reindex job was created but was not needed")
    }

    @Test
    fun testUpdateFieldSuggest() {
        val jobCount = jobService.getAll(JobFilter()).size()
        val spec = FieldSpec("File Extension", "foo.bar", AttrType.StringAnalyzed,
                keywords = true, suggest = false)
        val field = fieldSystemService.createField(spec)
        assertEquals(jobCount, jobService.getAll(JobFilter()).size(),
                "reindex job was created but was not needed")

        assertTrue(fieldSystemService.updateField(field, FieldUpdateSpec(field.name, field.editable,
                field.keywords, field.keywordsBoost, true)
        ))
        assertEquals(jobCount + 1, jobService.getAll(JobFilter()).size(),
                "reindex job was not created")
    }

    @Test
    fun testApplySuggestFields() {
        val spec = FieldSpec("File Extension", "source.extension", AttrType.StringAnalyzed,
                keywords = true, suggest = true)
        fieldSystemService.createField(spec, reindexSuggest = false)
        val assets = searchService.search(Pager.first(), AssetSearch()).list
        fieldSystemService.applySuggestions(assets)
        for (asset in assets) {
            assertTrue(asset.getAttr("system.suggestions", Json.LIST_OF_STRINGS).contains(
                    asset.getAttr("source.extension", String::class.java)))
        }
    }
}

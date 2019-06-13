package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.AttrType
import com.zorroa.archivist.domain.Field
import com.zorroa.archivist.domain.FieldFilter
import com.zorroa.archivist.domain.FieldSpec
import com.zorroa.archivist.domain.FieldUpdateSpec
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.service.event
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import com.zorroa.common.util.Json
import com.zorroa.common.util.readValueOrNull
import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

interface FieldDao {

    fun create(spec: FieldSpec): Field
    fun get(id: UUID): Field
    fun getAll(filter: FieldFilter): KPagedList<Field>
    fun findOne(filter: FieldFilter): Field
    fun count(filter: FieldFilter): Long
    fun get(attrName: String): Field
    fun exists(attrName: String): Boolean
    fun deleteAll(): Int
    fun delete(field: Field): Boolean
    fun update(field: Field, spec: FieldUpdateSpec): Boolean

    /**
     * Allocate a brand new field attribute.  This function picks a new custom
     * ES field name for the given attribute.  Once a field name is allocated,
     * it can never be reused.
     *
     * @param type: The field type to allocate for.
     */
    fun allocate(type: AttrType): String

    /**
     * Return a list of attribute names for use with keyword search.
     *
     * @param forExactMatch If the field type supports .raw, return that instead.
     * @return a map of attr name / boost value
     */
    fun getKeywordAttrNames(forExactMatch: Boolean = false): Map<String, Float>

    /**
     * Return a list of the fields marked for suggestions.
     */
    fun getSuggestAttrNames(): List<String>
}

@Repository
class FieldDaoImpl : AbstractDao(), FieldDao {

    override fun create(spec: FieldSpec): Field {

        /**
         * AttrType.StringSuggest is deprecated, when used just
         * convert it to a suggest + keywords field.
         */
        if (spec.attrType == AttrType.StringSuggest) {
            spec.attrType = AttrType.StringAnalyzed
            spec.suggest = true
            spec.keywords = true
        }

        /**
         * If it's a suggestion, its also a keyword since it would be
         * annoying to suggest something that doesn't produce a result.
         */
        if (spec.suggest) {
            spec.keywords = true
        }

        val time = System.currentTimeMillis()
        val id = uuid1.generate()
        val user = getUser()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, id)
            ps.setObject(2, user.organizationId)
            ps.setObject(3, user.id)
            ps.setObject(4, user.id)
            ps.setLong(5, time)
            ps.setLong(6, time)
            ps.setString(7, spec.name)
            ps.setString(8, spec.attrName)
            ps.setInt(9, spec.attrType!!.ordinal)
            ps.setBoolean(10, spec.editable)
            ps.setBoolean(11, spec.custom)
            ps.setBoolean(12, spec.keywords)
            ps.setFloat(13, spec.keywordsBoost)
            ps.setString(14, Json.serializeToString(spec.options, null))
            ps.setBoolean(15, spec.suggest)
            ps
        }

        logger.event(
            LogObject.FIELD, LogAction.CREATE, mapOf("fieldId" to id,
                "fieldName" to spec.name,
                "attrType" to spec.attrType,
                "attrName" to spec.attrName,
                "isKeywords" to spec.keywords,
                "isSuggest" to spec.suggest))

        return Field(id, spec.name, spec.attrName,
                spec.attrType, spec.editable,
                spec.custom, spec.keywords, spec.keywordsBoost,
                spec.suggest, spec.options)
    }

    override fun update(field: Field, spec: FieldUpdateSpec): Boolean {
        val time = System.currentTimeMillis()
        val user = getUser()

        /**
         * If it's a suggestion, its also a keyword since it would be
         * annoying to suggest something that doesn't produce a result.
         */
        if (spec.suggest) {
            spec.keywords = true
        }

        return jdbc.update { connection ->
            val ps = connection.prepareStatement("$UPDATE AND pk_organization=?")
            ps.setLong(1, time)
            ps.setObject(2, user.id)
            ps.setString(3, spec.name)
            ps.setBoolean(4, spec.editable)
            ps.setBoolean(5, spec.keywords)
            ps.setFloat(6, spec.keywordsBoost)
            ps.setString(7, Json.serializeToString(spec.options, null))
            ps.setBoolean(8, spec.suggest)
            ps.setObject(9, field.id)
            ps.setObject(10, user.organizationId)
            ps
        } == 1
    }

    override fun get(id: UUID): Field {
        return jdbc.queryForObject("$GET WHERE pk_field=? AND pk_organization=?",
                MAPPER, id, getOrgId())
    }

    override fun get(attrName: String): Field {
        return jdbc.queryForObject("$GET WHERE str_attr_name=? AND pk_organization=?",
                MAPPER, attrName, getOrgId())
    }

    override fun exists(attrName: String): Boolean {
        return jdbc.queryForObject("$COUNT WHERE str_attr_name=? AND pk_organization=?",
                Int::class.java, attrName, getOrgId()) == 1
    }

    override fun getAll(filter: FieldFilter): KPagedList<Field> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    override fun findOne(filter: FieldFilter): Field {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return jdbc.queryForObject(query, MAPPER, *values)
    }

    override fun getKeywordAttrNames(forExactMatch: Boolean): Map<String, Float> {
        val result = mutableMapOf<String, Float>()
        jdbc.query(GET_KEYWORD_FIELDS,
            RowCallbackHandler { rs ->
                val type = AttrType.values()[rs.getInt("int_attr_type")]
                if (forExactMatch && type == AttrType.StringAnalyzed) {
                    result[rs.getString("str_attr_name") + ".raw"] = rs.getFloat("float_keywords_boost")
                } else {
                    result[rs.getString("str_attr_name")] = rs.getFloat("float_keywords_boost")
                }
            }, getOrgId())
        return result
    }

    override fun getSuggestAttrNames(): List<String> {
        return jdbc.queryForList(
                "SELECT str_attr_name FROM field WHERE pk_organization=? AND bool_suggest='t'",
                String::class.java, getOrgId())
    }

    override fun count(filter: FieldFilter): Long {
        val query = filter.getQuery(COUNT, true)
        return jdbc.queryForObject(query, Long::class.java, *filter.getValues(true))
    }

    override fun deleteAll(): Int {
        return jdbc.update("DELETE FROM field WHERE pk_organization=?", getOrgId())
    }

    override fun delete(field: Field): Boolean {
        return jdbc.update("DELETE FROM field WHERE pk_field=?", field.id) == 1
    }

    override fun allocate(type: AttrType): String {
        val user = getUser()
        val num = if (jdbc.update(ALLOC_UPDATE, user.organizationId, type.ordinal) == 1) {
            jdbc.queryForObject(
                    "SELECT int_count FROM field_alloc WHERE pk_organization=? AND int_attr_type=?",
                    Int::class.java, user.organizationId, type.ordinal)
        } else {
            val id = uuid1.generate()
            jdbc.update(ALLOC_INSERT, id, user.organizationId, type.ordinal, 0)
            0
        }
        return type.getCustomAttrName(num)
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->

            Field(rs.getObject("pk_field") as UUID,
                    rs.getString("str_name"),
                    rs.getString("str_attr_name"),
                    AttrType.values()[rs.getInt("int_attr_type")],
                    rs.getBoolean("bool_editable"),
                    rs.getBoolean("bool_custom"),
                    rs.getBoolean("bool_keywords"),
                    rs.getFloat("float_keywords_boost"),
                    rs.getBoolean("bool_suggest"),
                    Json.Mapper.readValueOrNull(rs.getString("json_options")))
        }

        private const val GET = "SELECT * FROM field"
        private const val COUNT = "SELECT COUNT(1) FROM field"

        private val INSERT = JdbcUtils.insert("field",
                "pk_field",
                "pk_organization",
                "pk_user_created",
                "pk_user_modified",
                "time_created",
                "time_modified",
                "str_name",
                "str_attr_name",
                "int_attr_type",
                "bool_editable",
                "bool_custom",
                "bool_keywords",
                "float_keywords_boost",
                "json_options::jsonb",
                "bool_suggest")

        private val UPDATE = JdbcUtils.update("field", "pk_field",
                "time_modified",
                "pk_user_modified",
                "str_name",
                "bool_editable",
                "bool_keywords",
                "float_keywords_boost",
                "json_options::jsonb",
                "bool_suggest")

        private const val ALLOC_UPDATE = "UPDATE field_alloc " +
                "SET int_count=int_count + 1 " +
                "WHERE pk_organization=? " +
                "AND int_attr_type=?"

        private val ALLOC_INSERT = JdbcUtils.insert("field_alloc",
                "pk_field_alloc",
                "pk_organization",
                "int_attr_type",
                "int_count")

        private const val GET_KEYWORD_FIELDS =
            "SELECT " +
                "str_attr_name, " +
                "float_keywords_boost, " +
                "int_attr_type " +
            "FROM " +
                "field " +
            "WHERE " +
                "pk_organization=? " +
            "AND " +
                "bool_keywords='t'"
    }
}

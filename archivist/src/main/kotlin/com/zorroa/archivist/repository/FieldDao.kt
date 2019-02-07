package com.zorroa.archivist.repository
import com.zorroa.archivist.domain.*
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getUser
import com.zorroa.archivist.service.event
import com.zorroa.common.repository.KPagedList
import com.zorroa.common.util.JdbcUtils
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.*

interface FieldDao {

    /**
     * Allocate a brand new field attribute.  This function picks a new custom
     * ES field name for the given attribute.  Once a field name is allocated,
     * it can never be reused.
     *
     * @param type: The field type to allocate for.
     */
    fun allocate(type: AttrType) : String


    fun create(spec: FieldSpec) : Field
    fun get(id: UUID) : Field
    fun getAll(filter: FieldFilter?): KPagedList<Field>
    fun count(filter: FieldFilter): Long
    fun get(attrName: String) : Field
    fun exists(attrName: String) : Boolean
    fun deleteAll() : Int
}

@Repository
class FieldDaoImpl : AbstractDao(), FieldDao {

    override fun create(spec: FieldSpec) : Field {

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
            ps
        }

        logger.event(LogObject.FIELD, LogAction.CREATE,
                mapOf("fieldId" to id, "fieldName" to spec.name, "fieldAttrType" to spec.attrType))
        return Field(id, spec.name, spec!!.attrName as String,
                spec.attrType as AttrType, spec.editable, spec.custom)
    }

    override fun get(id: UUID) : Field {
        return jdbc.queryForObject("$GET WHERE pk_field=? AND pk_organization=?",
                MAPPER, id, getOrgId())
    }

    override fun get(attrName: String) : Field {
        return jdbc.queryForObject("$GET WHERE str_attr_name=? AND pk_organization=?",
                MAPPER, attrName, getOrgId())
    }

    override fun exists(attrName: String) : Boolean {
        return jdbc.queryForObject("$COUNT WHERE str_attr_name=? AND pk_organization=?",
                Int::class.java, attrName, getOrgId()) == 1
    }

    override fun getAll(filter: FieldFilter?): KPagedList<Field> {
        val filt = filter ?: FieldFilter()
        val query = filt.getQuery(GET, false)
        val values = filt.getValues(false)
        return KPagedList(count(filt), filt.page, jdbc.query(query, MAPPER, *values))
    }

    override fun count(filter: FieldFilter): Long {
        val query = filter.getQuery(COUNT, true)
        return jdbc.queryForObject(query, Long::class.java, *filter.getValues(true))
    }

    override fun deleteAll() : Int {
        return jdbc.update("DELETE FROM field WHERE pk_organization=?", getOrgId())
    }

    override fun allocate(type: AttrType) : String {
        val user = getUser()
        val num= if (jdbc.update(ALLOC_UPDATE, user.organizationId, type.ordinal) == 1) {
            jdbc.queryForObject(
                    "SELECT int_count FROM field_alloc WHERE pk_organization=? AND int_attr_type=?",
                    Int::class.java, user.organizationId, type.ordinal)
        }
        else {
            val id = uuid1.generate()
            jdbc.update(ALLOC_INSERT, id, user.organizationId, type.ordinal, 0)
            0
        }
        return type.attrName(num)
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            Field(rs.getObject("pk_field") as UUID,
                    rs.getString("str_name"),
                    rs.getString("str_attr_name"),
                    AttrType.values()[rs.getInt("int_attr_type")],
                    rs.getBoolean("bool_editable"),
                    rs.getBoolean("bool_custom"))
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
                "bool_custom")

        private const val ALLOC_UPDATE = "UPDATE field_alloc " +
                "SET int_count=int_count + 1 " +
                "WHERE pk_organization=? " +
                "AND int_attr_type=?"

        private val ALLOC_INSERT = JdbcUtils.insert("field_alloc",
                "pk_field_alloc",
                "pk_organization",
                "int_attr_type",
                "int_count")
    }

}

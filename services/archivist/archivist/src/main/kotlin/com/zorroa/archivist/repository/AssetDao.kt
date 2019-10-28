package com.zorroa.archivist.repository

import com.zorroa.archivist.domain.AssetState
import com.zorroa.archivist.domain.Document
import com.zorroa.archivist.security.getOrgId
import com.zorroa.archivist.security.getUser
import com.zorroa.common.util.JdbcUtils
import com.zorroa.common.util.JdbcUtils.inClause
import com.zorroa.common.util.Json
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.PreparedStatement
import java.sql.SQLException
import java.util.Date
import java.util.UUID

interface AssetDao {
    fun createOrReplace(asset: Document): Document
    fun batchCreateOrReplace(docs: List<Document>): Int
    fun get(id: UUID): Document
    fun get(id: String): Document
    fun getMap(ids: List<String>): Map<String, Document>
    fun getAll(ids: List<String>): List<Document>
    fun batchUpdate(docs: List<Document>): IntArray
}

@Repository
class AssetDaoImpl : AbstractDao(), AssetDao {

    override fun createOrReplace(doc: Document): Document {
        val time = extractTime(doc)
        val user = getUser()

        jdbc.update { connection ->
            val ps = connection.prepareStatement(INSERT)
            ps.setObject(1, UUID.fromString(doc.id))
            ps.setObject(2, user.organizationId)
            ps.setObject(3, user.id)
            ps.setObject(4, user.id)
            ps.setLong(5, time.time)
            ps.setLong(6, time.time)
            ps.setInt(7, AssetState.Active.ordinal)
            ps.setString(8, Json.serializeToString(doc.document, "{}"))
            ps
        }

        return doc
    }

    override fun batchCreateOrReplace(docs: List<Document>): Int {
        if (docs.isEmpty()) {
            return 0
        }

        val time = extractTime(docs[0])
        val user = getUser()
        val result = jdbc.batchUpdate(INSERT, object : BatchPreparedStatementSetter {

            @Throws(SQLException::class)
            override fun setValues(ps: PreparedStatement, i: Int) {
                val doc = docs[i]
                ps.setObject(1, UUID.fromString(doc.id))
                ps.setObject(2, user.organizationId)
                ps.setObject(3, user.id)
                ps.setObject(4, user.id)
                ps.setLong(5, time.time)
                ps.setLong(6, time.time)
                ps.setInt(7, AssetState.Active.ordinal)
                ps.setString(8, Json.serializeToString(doc.document, "{}"))
                ps
            }

            override fun getBatchSize(): Int {
                return docs.size
            }
        })
        return result.sum()
    }

    override fun batchUpdate(docs: List<Document>): IntArray {
        if (docs.isEmpty()) { return IntArray(0) }
        val time = extractTime(docs[0])
        val user = getUser()

        return jdbc.batchUpdate(UPDATE, object : BatchPreparedStatementSetter {

            @Throws(SQLException::class)
            override fun setValues(ps: PreparedStatement, i: Int) {
                val doc = docs[i]
                ps.setObject(1, user.id)
                ps.setLong(2, time.time)
                ps.setString(3, Json.serializeToString(doc.document, "{}"))
                ps.setObject(4, UUID.fromString(doc.id))
                ps.setObject(5, user.organizationId)
                ps
            }

            override fun getBatchSize(): Int {
                return docs.size
            }
        })
    }

    override fun get(id: String): Document {
        return get(UUID.fromString(id))
    }

    override fun get(id: UUID): Document {
        return jdbc.queryForObject(
                "SELECT pk_asset, json_document FROM asset WHERE pk_asset=? AND pk_organization=?",
                MAPPER, id, getOrgId())
    }

    override fun getAll(ids: List<String>): List<Document> {
        if (ids.isEmpty()) {
            return emptyList()
        }
        val where = inClause("pk_asset", ids.size, "uuid")
        val values = mutableListOf<Any>()
        values.add(getOrgId())
        values.addAll(ids)

        return jdbc.query("SELECT pk_asset, json_document FROM asset WHERE pk_organization=?::uuid AND $where",
                MAPPER, *values.toTypedArray())
    }

    override fun getMap(ids: List<String>): Map<String, Document> {
        if (ids.isEmpty()) {
            return emptyMap()
        }
        val where = inClause("pk_asset", ids.size, "uuid")
        val values = mutableListOf<Any>()
        values.add(getOrgId())
        values.addAll(ids)

        val result = mutableMapOf<String, Document>()
        jdbc.query(
                "SELECT pk_asset, json_document FROM asset WHERE pk_organization=? AND $where",
                {
                    val doc = MAPPER.mapRow(it, 0)
                    result[doc.id] = doc
                }, values.toTypedArray())

        return result
    }

    fun extractTime(doc: Document): Date {
        return doc.getAttr("system.timeModified", Date::class.java) ?: Date()
    }

    companion object {

        private val MAPPER = RowMapper { rs, _ ->
            Document(
                    rs.getString(1),
                    Json.deserialize(rs.getString(2), Json.GENERIC_MAP))
        }

        private val UPDATE = JdbcUtils.update("asset",
                "pk_asset",
                "pk_user_modified",
                "time_modified",
                "json_document::jsonb").plus(" AND pk_organization=?")

        private val INSERT = JdbcUtils.insert("asset",
                "pk_asset",
                "pk_organization",
                "pk_user_created",
                "pk_user_modified",
                "time_created",
                "time_modified",
                "int_state",
                "json_document::jsonb")
                .plus("ON CONFLICT (pk_asset) DO UPDATE SET json_document=EXCLUDED.json_document,")
                .plus("pk_user_modified=EXCLUDED.pk_user_modified,")
                .plus("time_modified=EXCLUDED.time_modified,")
                .plus("int_update_count=asset.int_update_count+1")
    }
}
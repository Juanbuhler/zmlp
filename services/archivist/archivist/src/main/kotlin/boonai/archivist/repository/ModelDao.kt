package boonai.archivist.repository

import boonai.archivist.domain.Model
import boonai.archivist.domain.ModelFilter
import boonai.archivist.domain.ModelState
import boonai.archivist.domain.ModelType
import boonai.archivist.security.getZmlpActor
import boonai.common.service.logging.LogAction
import boonai.common.service.logging.LogObject
import boonai.common.service.logging.event
import boonai.common.util.Json
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ModelDao : JpaRepository<Model, UUID> {

    fun getOneByProjectIdAndId(projectId: UUID, id: UUID): Model?
    fun existsByProjectIdAndId(projectId: UUID, id: UUID): Boolean
}

interface ModelJdbcDao {

    /**
     * Delete the model
     */
    fun delete(model: Model): Boolean

    /**
     * Find a [KPagedList] of [Model] that match the given [ModelFilter]
     * The [Project] filter is applied automatically.
     */
    fun find(filter: ModelFilter): KPagedList<Model>

    /**
     * Find one and only one [KPagedList] of [Model] that match
     * the given [ModelFilter]. The [Project] filter is applied automatically.
     */
    fun findOne(filter: ModelFilter): Model

    /**
     * Count the number of [Model] that match the given [ModelFilter]
     * The [Project] filter is applied automatically.
     */
    fun count(filter: ModelFilter): Long

    /**
     * Set the endpoint property on a model.
     */
    fun setEndpoint(modelId: UUID, value: String)

    /**
     * Set the model state and associated values.
     */
    fun updateState(modelId: UUID, state: ModelState)
}

@Repository
class ModelJdbcDaoImpl : AbstractDao(), ModelJdbcDao {

    override fun updateState(modelId: UUID, state: ModelState) {
        val time = System.currentTimeMillis()
        val actor = getZmlpActor().toString()

        logger.event(
            LogObject.MODEL, LogAction.STATE_CHANGE,
            mapOf("modelId" to modelId, "newState" to state.name)
        )

        when (state) {
            ModelState.Deploying -> {
                jdbc.update(
                    "UPDATE model SET int_state=?, time_last_uploaded=?, actor_last_uploaded=? WHERE pk_model=?",
                    state.ordinal, time, actor, modelId
                )
            }
            ModelState.Deployed -> {
                jdbc.update(
                    "UPDATE model SET int_state=?, time_last_deployed=?," +
                        " actor_last_deployed=actor_last_uploaded WHERE pk_model=?",
                    state.ordinal, time, modelId
                )
            }
            ModelState.Trained -> {
                jdbc.update(
                    "UPDATE model SET int_state=?, time_last_trained=?, actor_last_trained=? WHERE pk_model=?",
                    state.ordinal, time, actor, modelId
                )
            }
            else -> {
                jdbc.update(
                    "UPDATE model SET int_state=? WHERE pk_model=?",
                    state.ordinal, modelId
                )
            }
        }
    }

    override fun setEndpoint(modelId: UUID, value: String) {
        jdbc.update("UPDATE model SET str_endpoint=? WHERE pk_model=?", value, modelId)
    }

    override fun count(filter: ModelFilter): Long {
        return jdbc.queryForObject(
            filter.getQuery(COUNT, forCount = true),
            Long::class.java, *filter.getValues(forCount = true)
        )
    }

    override fun findOne(filter: ModelFilter): Model {
        filter.apply { page = KPage(0, 1) }
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return throwWhenNotFound("Model not found") {
            return KPagedList(1L, filter.page, jdbc.query(query, MAPPER, *values))[0]
        }
    }

    override fun delete(model: Model): Boolean {
        return jdbc.update("DELETE FROM model where pk_model=?", model.id) == 1
    }

    override fun find(filter: ModelFilter): KPagedList<Model> {
        val query = filter.getQuery(GET, false)
        val values = filter.getValues(false)
        return KPagedList(count(filter), filter.page, jdbc.query(query, MAPPER, *values))
    }

    private val MAPPER = RowMapper { rs, _ ->
        Model(
            rs.getObject("pk_model") as UUID,
            rs.getObject("pk_project") as UUID,
            rs.getObject("pk_dataset") as UUID?,
            ModelState.values()[rs.getInt("int_state")],
            ModelType.values()[rs.getInt("int_type")],
            rs.getString("str_name"),
            rs.getString("str_module"),
            rs.getString("str_endpoint"),
            rs.getString("str_file_id"),
            rs.getString("str_job_name"),
            rs.getBoolean("bool_trained"),
            Json.Mapper.readValue(rs.getString("json_apply_search"), Json.GENERIC_MAP),
            Json.Mapper.readValue(rs.getString("json_train_args"), Json.GENERIC_MAP),
            Json.Mapper.readValue(rs.getString("json_depends"), Json.LIST_OF_STRING),
            rs.getLong("time_created"),
            rs.getLong("time_modified"),
            rs.getString("actor_created"),
            rs.getString("actor_modified"),
            rs.getLong("time_last_trained"),
            rs.getString("actor_last_trained"),
            rs.getLong("time_last_applied"),
            rs.getString("actor_last_applied"),
            rs.getLong("time_last_tested"),
            rs.getString("actor_last_tested"),
            rs.getLong("time_last_uploaded"),
            rs.getString("actor_last_uploaded"),
            rs.getLong("time_last_deployed"),
            rs.getString("actor_last_deployed")
        )
    }

    companion object {

        const val GET = "SELECT * FROM model"
        const val COUNT = "SELECT COUNT(1) FROM model"
    }
}

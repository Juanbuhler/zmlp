package boonai.archivist.domain

import boonai.archivist.repository.KDaoFilter
import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID
import javax.persistence.AttributeConverter
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Converter
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

enum class TriggerType {
    AssetAnalyzed,
    AssetModified,
    AssetDeleted
}

@Converter
class TriggerConverter : AttributeConverter<Array<TriggerType>, String> {
    override fun convertToDatabaseColumn(attribute: Array<TriggerType>): String {
        return attribute.map { it.ordinal }.joinToString(",")
    }

    override fun convertToEntityAttribute(dbData: String): Array<TriggerType> {
        return dbData.split(",").map { TriggerType.values()[it.toInt()] }.toTypedArray()
    }
}

class WebHookSpec(

    @ApiModelProperty("The remote URL for the web hook.")
    val url: String,

    @ApiModelProperty("The secret token used by the webhook server to validate the request")
    val secretToken: String,

    @ApiModelProperty("The triggers the webhook should fire on.")
    val triggers: Array<TriggerType>,
)

class WebHookUpdate(

    @ApiModelProperty("The remote URL for the web hook.")
    val url: String,

    @ApiModelProperty("The secret token used by the webhook server to validate the request")
    val secretToken: String,

    @ApiModelProperty("The triggers the webhook should fire on.")
    val triggers: Array<TriggerType>,

    val active: Boolean
)

@Entity
@Table(name = "webhook")
@ApiModel("WebHook", description = "WebHooks ")
class WebHook(

    @Id
    @Column(name = "pk_webhook")
    @ApiModelProperty("The unique ID of the WebHook")
    val id: UUID,

    @Column(name = "pk_project")
    val projectId: UUID,

    @Column(name = "url")
    val url: String,

    @Column(name = "secret_token")
    val secretToken: String,

    @Convert(converter = TriggerConverter::class)
    @Column(name = "triggers")
    val triggers: Array<TriggerType>,

    @Column(name = "active")
    val active: Boolean,

    @Column(name = "time_created")
    @ApiModelProperty("The time the WebHook was created.")
    val timeCreated: Long,

    @Column(name = "time_modified")
    @ApiModelProperty("The last time the WebHook was modified.")
    val timeModified: Long,

    @Column(name = "actor_created")
    @ApiModelProperty("The key which created this WebHook")
    val actorCreated: String,

    @Column(name = "actor_modified")
    @ApiModelProperty("The key that last made the last modification to this WebHook")
    val actorModified: String
)

class WebHookFilter : KDaoFilter() {

    @ApiModelProperty("WebHook IDs to match.")
    val ids: List<UUID>? = null

    @JsonIgnore
    override val sortMap: Map<String, String> =
        mapOf(
            "id" to "webhook.pk_webhook",
            "timeCreated" to "webhook.time_created",
            "timeModified" to "webhook.time_modified"
        )

    @JsonIgnore
    override fun build() {
    }
}

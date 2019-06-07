package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.repository.LongRangeFilter
import com.zorroa.archivist.security.getOrgId
import com.zorroa.common.repository.KDaoFilter
import com.zorroa.common.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

/**
 * The type of audit log entry.
 */
enum class AuditLogType {
    /**
     * The initial creation of an asset.
     */
    Created,

    /**
     * The asset was replaced.
     */
    Replaced,

    /**
     * The asset was deleted.
     */
    Deleted,

    /**
     * Permissions on the asset changed.
     */
    Secured,

    /**
     * A field on an asset changed in some way.
     */
    Changed,

    /**
     * A Warning message concerning the asset.
     */
    Warning
}

@ApiModel("Audit Log Filter", description = "Used to filter an audit log query.")
class AuditLogFilter(

    @ApiModelProperty("Asset UUIDs to match.")
    val assetIds: List<UUID>? = null,

    @ApiModelProperty("User UUIDs to match,")
    val userIds: List<UUID>? = null,

    @ApiModelProperty("Field UUIDs to match.")
    val fieldIds: List<UUID>? = null,

    @ApiModelProperty("Time range to filter on.")
    val timeCreated: LongRangeFilter? = null,

    @ApiModelProperty("Types to match.")
    val types: List<AuditLogType>? = null,

    @ApiModelProperty("Attribute names to match.")
    val attrNames: List<String>? = null

) : KDaoFilter() {

    @JsonIgnore
    override val sortMap: Map<String, String> =
            mapOf("timeCreated" to "time_created",
                    "userId" to "auditlog.pk_user_created",
                    "assetId" to "auditlog.pk_asset",
                    "fieldId" to "auditlog.pk_field",
                    "types" to "auditlog.int_type",
                    "attrName" to "auditlog.str_attr_name")

    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("timeCreated:desc")
        }

        assetIds?.let {
            addToWhere(JdbcUtils.inClause("auditlog.pk_asset", it.size))
            addToValues(it)
        }

        userIds?.let {
            addToWhere(JdbcUtils.inClause("auditlog.pk_user_created", it.size))
            addToValues(it)
        }

        fieldIds?.let {
            addToWhere(JdbcUtils.inClause("auditlog.pk_field", it.size))
            addToValues(it)
        }

        timeCreated?.let {
            addToWhere(JdbcUtils.rangeClause("auditlog.time_created", it))
            addToValues(it.getFilterValues())
        }

        types?.let {
            addToWhere(JdbcUtils.inClause("auditlog.int_type", it.size))
            addToValues(it.map { t -> t.ordinal })
        }

        attrNames?.let {
            addToWhere(JdbcUtils.inClause("auditlog.str_attr_name", it.size))
            addToValues(it)
        }

        addToWhere("pk_organization=?")
        addToValues(getOrgId())
    }
}

@ApiModel("Audit Log Entry", description = "Describes an action taken by a User.")
class AuditLogEntry(

    @ApiModelProperty("UUID of the Audit Log Entry.")
    val id: UUID,

    @ApiModelProperty("UUID of the Asset that was modified.")
    val assetId: UUID,

    @ApiModelProperty("UUID of the Field that was modified.")
    val fieldId: UUID?,

    @ApiModelProperty("User that took the action.")
    val user: UserBase,

    @ApiModelProperty("Time the entry was created.")
    val timeCreated: Long,

    @ApiModelProperty("Type of action.")
    val type: AuditLogType,

    @ApiModelProperty("Name of the attribute that changed.")
    val attrName: String?,

    @ApiModelProperty("Message associated with the log entry.")
    val message: String?,

    @ApiModelProperty("New value of a field or property changed.")
    val value: Any?

)

/**
 * The properties required to create an audit log entry.
 *
 * @property assetId The ID of the asset.
 * @property type The type of entry. See the AuditLogType enum.
 * @property fieldId The fieldId associated with the log entry.  Can be null.
 * @property message The log message.  If null, a log message will be auto-generated.
 * @property attrName The attribute name associated with the log entry.
 * @property value The new value of a field or property changed.  Can be null.
 * @property scope The scope/sub-type of the entry.  For example, type=Changed can happen in
 * many different places, scope describes the place it occurred.
 */
class AuditLogEntrySpec(
    val assetId: UUID,
    val type: AuditLogType,
    val fieldId: UUID? = null,
    val message: String? = null,
    val attrName: String? = null,
    val value: Any? = null,
    val scope: String? = null
) {
    constructor(
        assetId: String,
        type: AuditLogType,
        fieldId: UUID? = null,
        message: String? = null,
        attrName: String? = null,
        value: Any? = null,
        scope: String? = null
    ) :
            this(UUID.fromString(assetId), type, fieldId, message, attrName, value, scope)
}

package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zorroa.archivist.repository.KDaoFilter
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.JdbcUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

/**
 * Type of models that can be trained.
 */
enum class ModelType(
    val processor: String,
    val args: Map<String, Any>,
    val moduleName: String
) {
    FAST_CLASSIFICATON("zmlp_train.kmeans.KMeansTrainer",
        mapOf(),
        "custom-%s-fast-classification"),
    TF2_XFER_RESNET152(
        "zmlp_train.tf2.TensorflowTransferLearningTrainer",
        mapOf(
            "min_concepts" to 2,
            "min_examples" to 5,
            "train-test-ratio" to 3
        ),
        "custom-%s-label-detection-resnet152"
    ),
    TF2_XFER_VGG16(
        "zmlp_train.tf2.TensorflowTransferLearningTrainer",
        mapOf(
            "min_concepts" to 2,
            "min_examples" to 5,
            "train-test-ratio" to 3
        ),
        "custom-%s-label-detection-vgg16"
    ),
    TF2_XFER_MOBILENET2(
        "zmlp_train.tf2.TensorflowTransferLearningTrainer",
        mapOf(
            "min_concepts" to 2,
            "min_examples" to 5,
            "train-test-ratio" to 3
        ),
        "custom-%s-label-detection-mobilenet2"
    )
}

class ModelTrainingArgs(
    val publish: Boolean = true
)

class ModelSpec(
    val dataSetId: UUID,
    val type: ModelType

)

@Entity
@Table(name = "model")
@ApiModel("Model", description = "A model can be trained from a DataSet")
class Model(

    @Id
    @Column(name = "pk_model")
    @ApiModelProperty("The unique ID of the DataSet")
    val id: UUID,

    @Column(name = "pk_project")
    val projectId: UUID,

    @Column(name = "pk_data_set")
    val dataSetId: UUID,

    @Column(name = "int_type")
    val type: ModelType,

    @Column(name = "str_name")
    @ApiModelProperty("The name of the Pipeline Module that will be created when training is complete")
    val name: String,

    @Column(name = "str_file_id")
    val fileId: String,

    @Column(name = "str_job_name")
    val trainingJobName: String,

    @Column(name = "bool_trained")
    @ApiModelProperty("True if the model is trained.")
    val ready: Boolean,

    @Column(name = "time_created")
    @ApiModelProperty("The time the Model was created.")
    val timeCreated: Long,

    @Column(name = "time_modified")
    @ApiModelProperty("The last time the Model was modified.")
    val timeModified: Long,

    @Column(name = "actor_created")
    @ApiModelProperty("The key which created this Model")
    val actorCreated: String,

    @Column(name = "actor_modified")
    @ApiModelProperty("The key that last made the last modification to this Model")
    val actorModified: String

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Model) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

@ApiModel("Model Filter", description = "A search filter for Models")
class ModelFilter(

    @ApiModelProperty("The Model IDs to match.")
    val ids: List<UUID>? = null,

    @ApiModelProperty("The DataSet IDs to match.")
    val dataSetIds: List<UUID>? = null,

    @ApiModelProperty("The Model names to match")
    val names: List<String>? = null,

    @ApiModelProperty("The Model types to match")
    val types: List<ModelType>? = null

) : KDaoFilter() {
    @JsonIgnore
    override val sortMap: Map<String, String> = mapOf(
        "name" to "model.str_name",
        "timeCreated" to "model.time_created",
        "timeModified" to "model.time_modified",
        "id" to "model.pk_data_set",
        "type" to "model.int_type")

    @JsonIgnore
    override fun build() {

        if (sort.isNullOrEmpty()) {
            sort = listOf("name:asc")
        }

        addToWhere("model.pk_project=?")
        addToValues(getProjectId())

        ids?.let {
            addToWhere(JdbcUtils.inClause("model.pk_model", it.size))
            addToValues(it)
        }

        dataSetIds?.let {
            addToWhere(JdbcUtils.inClause("model.pk_data_set", it.size))
            addToValues(it)
        }

        names?.let {
            addToWhere(JdbcUtils.inClause("model.str_name", it.size))
            addToValues(it)
        }

        types?.let {
            addToWhere(JdbcUtils.inClause("model.int_type", it.size))
            addToValues(it.map { t -> t.ordinal })
        }
    }
}

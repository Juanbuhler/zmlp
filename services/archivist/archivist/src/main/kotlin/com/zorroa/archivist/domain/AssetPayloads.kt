package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@ApiModel("Update Asset By Query Request", description = "Request structure to update an Asset.")
class UpdateAssetsByQueryRequest(

    @ApiModelProperty("A query to select assets")
    val query: Map<String, Any>? = null,

    @ApiModelProperty("A script to run on the doc")
    val script: Map<String, Any>? = null
)

@ApiModel("Update Asset Request", description = "Request structure to update an Asset.")
class UpdateAssetRequest(

    @ApiModelProperty("Key/value pairs to be updated.")
    val doc: Map<String, Any>? = null,

    @ApiModelProperty("A script to run on the doc")
    val script: Map<String, Any>? = null
)

@ApiModel(
    "Batch Upload Assets Request",
    description = "Defines the properties required to batch upload a list of assets."
)
class BatchUploadAssetsRequest(

    @ApiModelProperty("A list of AssetSpec objects which define the Assets starting metadata.")
    var assets: List<AssetSpec>,

    @ApiModelProperty(
        "Set to true if the assets should undergo " +
            "further analysis, or false to stay in the provisioned state."
    )
    val analyze: Boolean = true,

    @ApiModelProperty("The pipeline modules to execute if any, otherwise utilize the default Pipeline.")
    val modules: List<String>? = null,

    @ApiModelProperty("A list of available credentials for the analysis job.")
    val credentials: Set<String>? = null

) {

    lateinit var files: Array<MultipartFile>
}

@ApiModel("Batch Create Assets Request",
    description = "Defines the properties necessary to provision a batch of assets.")
class BatchCreateAssetsRequest(

    @ApiModelProperty("The list of assets to be created")
    val assets: List<AssetSpec>,

    @ApiModelProperty("Set to true if the assets should undergo " +
        "further analysis, or false to stay in the provisioned state.")
    val analyze: Boolean = true,

    @ApiModelProperty("The pipeline modules to execute if any, otherwise utilize the default Pipeline.")
    val modules: List<String>? = null,

    @ApiModelProperty("A list of available credentials for the analysis job.")
    val credentials: Set<String>? = null,

    @JsonIgnore
    @ApiModelProperty("The taskId that is creating the assets via expand.", hidden = true)
    val task: InternalTask? = null
)

@ApiModel("Batch Create Assets Response",
    description = "The response returned after provisioning assets.")
class BatchCreateAssetsResponse(

    @ApiModelProperty("A map of failed asset ids to error message")
    val failed: List<Map<String, String?>>,

    @ApiModelProperty("A list of asset Ids created.")
    val created: List<String>,

    @ApiModelProperty("The assets that already existed.")
    val exists: Collection<String>,

    @ApiModelProperty("The ID of the analysis job, if analysis was selected")
    var jobId: UUID? = null
)
{

    @ApiModelProperty("The total number of assets to be updated.")
    val totalUpdated = created.size + exists.size
}

@ApiModel("Batch Process Asset Search Request",
    description = "Batch reprocess and asset search.")
class ReprocessAssetSearchRequest(

    @ApiModelProperty("An ElasticSearch query to process.  All assets will be processed that match the search.")
    val search: Map<String, Any>,

    @ApiModelProperty("The modules to apply.")
    val modules: List<String>,

    @ApiModelProperty("The number of assets to run per batch.")
    val batchSize: Int = 64
)

@ApiModel("Batch Process Asset Search Response",
    description = "The reponse to a ReprocessAssetSearchRequest")
class ReprocessAssetSearchResponse(

    @ApiModelProperty("The job running the reprocess")
    val job: Job,

    @ApiModelProperty("The number of assets expected to be reprocessed.")
    val assetCount: Long
)

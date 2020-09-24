package com.zorroa.archivist.domain

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import org.elasticsearch.action.bulk.BulkResponse
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.Base64

@ApiModel("ClipSpec", description = "Properties for defining a video clip.")
class ClipSpec(

    @ApiModelProperty("The starting point of the video clip")
    val start: BigDecimal,

    @ApiModelProperty("The stopping point of the video clip")
    val stop: BigDecimal,

    @ApiModelProperty("The content contained in the video clip.")
    val content: List<String>,

    @ApiModelProperty("The confidence score that the content is correct.")
    val score: Double
)

@ApiModel("TrackSpec", description = "Properties for defining a timeline Track. Tracks contain clips.")
class TrackSpec(

    @ApiModelProperty("The name of the track")
    val name: String,

    @ApiModelProperty("The list of clips in the track.")
    val clips: List<ClipSpec>
)

@ApiModel("TimelineSpec", description = "A TimelineSpec is used to batch create video clips.")
class TimelineSpec(

    @ApiModelProperty("The AssetId to attach clips to.")
    val assetId: String,

    @ApiModelProperty("The name of the timeline")
    val name: String,

    @ApiModelProperty("A list of tracks.")
    val tracks: List<TrackSpec>,

    @ApiModelProperty("If the thumbnails should be generated for the timeline.")
    val generateThumbnails: Boolean = true
)

@ApiModel("CreateClipFailure", description = "A clip creation error.")
class CreateClipFailure(
    @ApiModelProperty("The ID of clip that failed.")
    val id: String,

    @ApiModelProperty("The failure message.")
    val message: String,
)

@ApiModel("CreateTimelineResponse", description = "The response sent after creating video clips with a timeline")
class CreateTimelineResponse(

    @ApiModelProperty("The AssetId")
    var assetId: String,

    @ApiModelProperty("The number of clips created")
    var created: Long = 0,

    @ApiModelProperty("The number of clips that failed to be created")
    var failed: MutableList<CreateClipFailure> = mutableListOf()
) {
    fun handleBulkResponse(rsp: BulkResponse) {
        if (rsp.hasFailures()) {
            created += rsp.items.count { !it.isFailed }
            rsp.items.filter { it.isFailed }.forEach {
                failed.add(CreateClipFailure(it.id, it.failureMessage))
            }
        } else {
            created += rsp.items.size
        }
    }
}

/**
 * A class for determining a video clip unique Id.
 */
class ClipIdBuilder(
    val asset: Asset,
    val timeline: String,
    val track: String,
    val clip: ClipSpec
) {

    fun buildId(): String {
        /**
         * Nothing about the order of these statements
         * can ever change or duplicate assets will be
         * created.
         */
        val digester = MessageDigest.getInstance("SHA-256")
        digester.update(asset.id.toByteArray())
        digester.update(timeline.toByteArray())
        digester.update(track.toByteArray())
        digester.update(clip.start.setScale(3, RoundingMode.HALF_UP).toString().toByteArray())
        digester.update(clip.stop.setScale(3, RoundingMode.HALF_UP).toString().toByteArray())

        // Clamp the size to 32, 48 is bit much and you still
        // get much better resolution than a UUID.  We could
        // also up it on shared indexes but probably not necessary.
        return Base64.getUrlEncoder()
            .encodeToString(digester.digest()).trim('=').substring(0, 32)
    }
}

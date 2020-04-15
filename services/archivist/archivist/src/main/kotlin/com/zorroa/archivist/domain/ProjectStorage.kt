package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.util.FileUtils
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.util.UUID

/**
 * Internal enum class which describes the file storage entity,
 * such as an Assets, Models, Jobs, tasks.  The name matches
 * the name used in the REST API.
 */
enum class ProjectStorageEntity {

    /**
     * The stored file is associated with an asset.
     */
    ASSET,

    /**
     * The stored file is associated with a dataset.
     */
    DATASET;

    /**
     * The name of the entity used in file paths or unique Ids.  Note the trailing 's'.
     */
    fun pathName() = "${name.toLowerCase()}s"

    companion object {

        /**
         * Finds the ProjectStorageEntity using a name like 'assets'.
         */
        fun find(name: String): ProjectStorageEntity {
            return valueOf(name.toUpperCase().trimEnd('S'))
        }
    }
}

/**
 * Internal class for commonly use storage categories.   Processors can pass up
 * whatever they want but these are used by the backend.
 */
object ProjectStorageCategory {

    /**
     * The file is considered a source file.  These are typically added by file uploads."
     */
    const val SOURCE = "source"

    /**
     * The file is a proxy or alternative low resolution representation."
     */
    const val PROXY = "proxy"

    /**
     * The file is a model to be used with classification.
     */
    const val MODEL = "model"
}

@ApiModel("Project Storage Request", description = "Properties needed to store a file into ProjectStorage.")
class ProjectStorageRequest(

    @ApiModelProperty("The entity the file is related to.")
    var entity: ProjectStorageEntity,

    @ApiModelProperty("The Id the entity the file is related to.")
    var entityId: String,

    @ApiModelProperty("The name of the file, overrides the local file name.")
    var name: String,

    @ApiModelProperty("The category of the file.")
    var category: String,

    @ApiModelProperty("Arbitrary attrs associated with file.")
    var attrs: Map<String, Any> = mapOf()
)

/**
 * The ProjectStorageLocator Interface defines the based properties needed
 * for a project storage locator. A Locator handles converting properties
 * into an actual bucket path.
 */
interface ProjectStorageLocator {

    /**
     * The category is the final directory before the file.
     */
    val category: String

    /**
     * The actual name of the file.
     */
    val name: String

    /**
     * The full path into bucket storage where the file is stored.
     */
    fun getPath(): String

    /**
     * A partial URI path that can be used with the Archivist URI.
     */
    fun getFileId(): String
}

/**
 * The properties required to locate a file.
 */
class ProjectFileLocator(
    val entity: ProjectStorageEntity,
    val entityId: String,
    override val category: String,
    override val name: String,
    @JsonIgnore
    val projectId: UUID? = null
) : ProjectStorageLocator {

    override fun getPath(): String {
        val pid = projectId ?: getProjectId()
        return "projects/$pid/${entity.pathName()}/$entityId/$category/$name"
    }

    override fun getFileId(): String {
        return "${entity.pathName()}/$entityId/$category/$name"
    }
}

/**
 * Internal class for storing a file against a [ProjectStorageLocator]
 *
 * @property locator The location of the file.
 * @property attrs Arbitrary attrs to store with the file.
 * @property data The actual file data in the form of a ByteArray
 * @property mimetype The mimetype (aka MediaType) of the file which is auto detected.
 */
class ProjectStorageSpec(
    val locator: ProjectStorageLocator,
    var attrs: Map<String, Any>,
    val data: ByteArray

) {
    val mimetype = FileUtils.getMediaType(locator.getPath())
}

@ApiModel("FileStorage", description = "Describes a file stored in ZMLP storage.")
class FileStorage(

    @ApiModelProperty("The unique ID of the file.")
    val id: String,

    @ApiModelProperty("The file name")
    val name: String,

    @ApiModelProperty("The category of file.")
    val category: String,

    @ApiModelProperty("The mimetype of the file, detected from the extension.")
    val mimetype: String,

    @ApiModelProperty("The size of the file.")
    val size: Long,

    @ApiModelProperty("Arbitrary attributes for the file")
    var attrs: Map<String, Any>,

    @ApiModelProperty("Overrides which Asset")
    var sourceAssetId: String? = null
) {
    companion object {
        val JSON_LIST_OF: TypeReference<List<FileStorage>> = object : TypeReference<List<FileStorage>>() {}
    }
}

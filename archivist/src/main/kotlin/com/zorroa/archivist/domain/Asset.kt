package com.zorroa.archivist.domain

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.NameBasedGenerator
import com.google.common.base.MoreObjects
import com.zorroa.archivist.search.AssetSearch
import com.zorroa.common.util.Json
import org.slf4j.LoggerFactory
import java.util.*
import java.util.regex.Pattern

/**
 * BatchUpdateAssetsRequest defines how to batch update a list of assets.
 *
 * The attributes property should be in dot notation, for example:
 * { "foo.bar" : 1, "source.ext": "png"}
 *
 * @property batch : Any array of asset ids.
 */
class BatchUpdateAssetsRequest(
        val batch: Map<String, Map<String, Any?>>
)
{
    fun size(): Int = batch.size

    override fun toString() : String {
        return "<BatchUpdateAssetRequet assetIds=${batch.keys}"
    }
}

/**
 * A response object for batch updating large numbers of assets via REST API.
 * Batch updates are able to edit individual attributes however the entire
 * document is rewritten.
 *
 * @property updatedAssetIds : The asset Ids updated
 * @property erroredAssetIds : The missing or errored asset Ids
 *
 */
class BatchUpdateAssetsResponse {
    val updatedAssetIds = mutableSetOf<String>()
    val erroredAssetIds = mutableSetOf<String>()

    operator fun plus(other: BatchUpdateAssetsResponse) {
        updatedAssetIds.addAll(other.updatedAssetIds)
        erroredAssetIds.addAll(other.erroredAssetIds)
    }

    override fun toString() : String {
        return "<BatchUpdateAssetsResponse updated=${updatedAssetIds.size} errored=${erroredAssetIds.size}>"
    }
}

/**
 * Request to update selected assets with new permissions.  If replace=true, then
 * all permissions are replaced,otherwise they are updated.
 *
 * @param search: An asset search
 * @param acl: An acl to apply
 * @param replace: Replace all permissions with this acl, defaults to false.
 */
class BatchUpdatePermissionsRequest(
        val search: AssetSearch,
        val acl: Acl,
        val replace: Boolean = false
)

/**
 * A response object for a BatchUpdatePermissionsRequest
 *
 * @property updatedAssetIds The asset ids that were updated.
 * @property errors A map of errors which happened during processing
 */
class BatchUpdatePermissionsResponse {

    val updatedAssetIds = mutableSetOf<String>()
    val errors = mutableMapOf<String, String>()

    operator fun plus(other: BatchCreateAssetsResponse) {
        updatedAssetIds.addAll(other.replacedAssetIds)
    }
}

/**
 * Structure for upserting a batch of assets.
 *
 * @property sources: The source documents
 * @property jobId: The associated job Id
 * @property taskID: The associated task Id
 * @property skipAssetPrep: Skip over asset prep stage during create.
 */
class BatchCreateAssetsRequest(
        val sources: List<Document>,
        val jobId: UUID?,
        val taskId: UUID?) {


    @JsonIgnore
    var skipAssetPrep = false

    @JsonIgnore
    var scope = "index"

    var isUpload = false

    constructor(sources: List<Document>, scope : String="index", skipAssetPrep:Boolean=false)
            : this(sources, null, null) {
        this.scope = scope
        this.skipAssetPrep = skipAssetPrep
    }
}


/**
 * The response after batch creating an array of assets.
 * @property createdAssetIds An array of asset ids that were created.
 * @property replacedAssetIds An array of asset ids that were replaced.
 * @property erroredAssetIds An array of asset ids that were an error and were not added.
 * @property warningAssetIds Asset IDs with a field warning.
 * @property retryCount Number of retries it took to get this batch through.
 */
class BatchCreateAssetsResponse(val total: Int) {
    var createdAssetIds = mutableSetOf<String>()
    var replacedAssetIds  = mutableSetOf<String>()
    var erroredAssetIds  = mutableSetOf<String>()
    var warningAssetIds = mutableSetOf<String>()
    var retryCount = 0

    fun add(other: BatchCreateAssetsResponse): BatchCreateAssetsResponse {
        createdAssetIds.addAll(other.createdAssetIds)
        replacedAssetIds.addAll(other.replacedAssetIds)
        erroredAssetIds.addAll(other.erroredAssetIds)
        warningAssetIds.addAll(other.warningAssetIds)
        retryCount += other.retryCount
        return this
    }

    /**
     * Return true if assets were created or replaced.
     */
    fun assetsChanged() : Boolean {
        return createdAssetIds.isNotEmpty() || replacedAssetIds.isNotEmpty()
    }

    /**
     * The total number of assets either created or replaced
     */
    fun totalAssetsChanged() : Int {
        return createdAssetIds.size + replacedAssetIds.size
    }

    override fun toString(): String {
        return MoreObjects.toStringHelper(this)
                .add("created", createdAssetIds.size)
                .add("replaced", replacedAssetIds.size)
                .add("warnings", warningAssetIds)
                .add("errors", erroredAssetIds.size)
                .add("retries", retryCount)
                .toString()
    }
}
/**
 * A request to batch delete assets.
 *
 * @property assetIds an array of assetIds to delete.
 */
class BatchDeleteAssetsRequest (
        val assetIds: List<String>
)

/**
 * The response returned when assets are deleted.
 *
 * @property totalRequested The total number of assets requested to be deleted.  This includes the resolved # of clips.
 * @property deletedAssetIds The total number of assets actually deleted.
 * @property onHoldAssetIds Assets skipped due to being on hold.
 * @property accessDeniedAssetIds Assets skipped due to permissions
 * @property missingAssetIds Assets that have already been deleted.
 * @property errors A map AssetID/Message failures.
 */
class BatchDeleteAssetsResponse (
    var totalRequested: Int=0,
    var deletedAssetIds: MutableSet<String> = mutableSetOf(),
    var onHoldAssetIds: MutableSet<String> = mutableSetOf(),
    var accessDeniedAssetIds: MutableSet<String> = mutableSetOf(),
    var missingAssetIds: MutableSet<String> = mutableSetOf(),
    var errors: MutableMap<String, String> = mutableMapOf()
) {
    operator fun plus(other: BatchDeleteAssetsResponse) {
        totalRequested += other.totalRequested
        deletedAssetIds.addAll(other.deletedAssetIds)
        onHoldAssetIds.addAll(other.onHoldAssetIds)
        accessDeniedAssetIds.addAll(other.accessDeniedAssetIds)
        missingAssetIds.addAll(other.missingAssetIds)
        errors.putAll(other.errors)
    }
}

enum class AssetState {
    /**
     * The default state for an Asset.
     */
    Active,

    /**
     * The asset has been deleted from ES.
     */
    Deleted
}

/**
 * The ES document
 */
open class Document {

    var document: Map<String, Any>

    var id: String = UUID.randomUUID().toString()

    var type = "asset"

    var permissions: MutableMap<String, Int>? = null

    var links: MutableList<Tuple<String, Any>>? = null

    var score : Float? = null

    var replace = false

    constructor() {
        document = mutableMapOf()
    }

    constructor(doc: Document) {
        this.id = doc.id
        this.type = doc.type
        this.document = doc.document
    }

    constructor(id: String, doc: Map<String, Any>) {
        this.id = id
        this.document = doc
    }

    constructor(id: String) {
        this.id = id
        this.document = mutableMapOf()
    }

    constructor(id: UUID) {
        this.id = id.toString()
        this.document = mutableMapOf()
    }

    constructor(doc: Map<String, Any>) {
        this.document = doc
    }


    fun addToLinks(type: String, id: Any): Document {
        if (links == null) {
            links = mutableListOf()
        }
        links?.apply {
            this.add(Tuple(type, id))
        }
        return this
    }

    fun addToPermissions(group: String, access: Int): Document {
        if (permissions == null) {
            permissions = mutableMapOf()
        }
        permissions?.apply {
            this[group] = access
        }
        return this
    }


    /**
     * Get an attribute value  by its fully qualified name.
     *
     * @param attr
     * @param <T>
     * @return
    </T> */
    fun <T> getAttr(attr: String): T? {
        val current = getContainer(attr, false)
        return getChild(current, Attr.name(attr)) as T?
    }

    /**
     * Get an attribute value by its fully qualified name.  Uses a JSON mapper
     * to map the data into the specified class.
     *
     * @param attr
     * @param type
     * @param <T>
     * @return
    </T> */
    fun <T> getAttr(attr: String, type: Class<T>): T {
        val current = getContainer(attr, false)
        return Json.Mapper.convertValue(getChild(current, Attr.name(attr)), type)
    }

    /**
     * Get an attribute value by its fully qualified name.  Uses a JSON mapper
     * to map the data into the specified TypeReference.
     *
     * @param attr
     * @param type
     * @param <T>
     * @return
    </T> */
    fun <T> getAttr(attr: String, type: TypeReference<T>): T {
        val current = getContainer(attr, false)
        return Json.Mapper.convertValue(getChild(current, Attr.name(attr)), type)
    }

    /**
     * Assumes the target attribute is a collection of some sort and tries to add
     * the given value.
     *
     * @param attr
     * @param value
     */
    fun addToAttr(attr: String, value: Any) {
        val current = getContainer(attr, true)
        val key = Attr.name(attr)


        /**
         * Handle the case where the object is a standard map.
         */
        try {
            val map = current as MutableMap<String, Any>?
            var collection: MutableCollection<Nothing>? = map!![key] as MutableCollection<Nothing>
            if (collection == null) {
                collection = mutableListOf()
                map[key] = collection
            }
            if (value is Collection<*>) {
                collection!!.addAll(value as Collection<Nothing>)
            } else {
                collection!!.add(value as Nothing)
            }
            return
        } catch (ex2: Exception) {
            logger.warn("The parent attribute {} of type {} is not valid.",
                    attr, current!!.javaClass.name)
        }

    }

    /**
     * Set an attribute value.
     *
     * @param attr
     * @param value
     */
    fun setAttr(attr: String, value: Any?) {
        val current = getContainer(attr, true)
        val key = Attr.name(attr)

        try {
            (current as MutableMap<String, Any>)[key] = Json.Mapper.convertValue(value as Any)
        } catch (ex: ClassCastException) {
            throw IllegalArgumentException("Invalid attribute: $attr", ex)
        }
    }

    /**
     * Remove an attribute.  If the attr cannot be remove it is set to null.
     *
     * @param attr
     */
    fun removeAttr(attr: String): Boolean {
        val current = getContainer(attr, true)
        val key = Attr.name(attr)

        /*
         * Finally, just try treating it like a map.
         */
        try {
            return (current as MutableMap<String, Any>).remove(key) != null
        } catch (ex: ClassCastException) {
            throw IllegalArgumentException("Invalid attribute: $attr")
        }
    }

    /**
     * Return true if the document has the given namespace.
     * @param attr
     * @return
     */
    fun attrExists(attr: String): Boolean {
        val container = getContainer(attr, false)
        return getChild(container, Attr.name(attr)) != null
    }

    /**
     * Return true if the value of an attribute contains the given value.
     *
     * @param attr
     * @return
     */
    fun attrContains(attr: String, value: Any): Boolean {
        val parent = getContainer(attr, false)
        val child = getChild(parent, Attr.name(attr))

        if (child is Collection<*>) {
            return child.contains(value)
        } else if (child is String) {
            return child.contains(value.toString())
        }
        return false
    }

    private fun getContainer(attr: String, forceExpand: Boolean): Any? {
        val parts = PATTERN_ATTR.split(attr)

        var current: Any? = document
        for (i in 0 until parts.size - 1) {

            var child = getChild(current, parts[i])
            if (child == null) {
                if (forceExpand) {
                    child = createChild(current, parts[i])
                } else {
                    return null
                }
            }
            current = child
        }
        return current
    }

    private fun getChild(`object`: Any?, key: String): Any? {
        if (`object` == null) {
            return null
        }
        try {
            return (`object` as Map<String, Any>)[key]
        } catch (ex: ClassCastException) {
            return null
        }
    }

    private fun createChild(parent: Any?, key: String): Any {
        val result = mutableMapOf<String, Any>()
        try {
            (parent as MutableMap<String, Any>)[key] = result
        } catch (ex: ClassCastException) {
            throw IllegalArgumentException("Invalid attribute: $key parent: $parent")
        }
        return result
    }

    override fun toString(): String {
        return "<Document $id - $document>"
    }

    companion object {

        private val logger = LoggerFactory.getLogger(Document::class.java)

        private val PATTERN_ATTR = Pattern.compile(Attr.DELIMITER, Pattern.LITERAL)
    }
}


object Attr {

    val DELIMITER = "."

    /**
     * A convenience method which takes a variable list of strings and
     * turns it into an attribute name.  This is preferred over using
     * string concatenation.
     *
     * @param name
     * @return
     */
    fun attr(vararg name: String): String {
        return name.joinToString(DELIMITER)
    }

    /**
     * Return the last part of an attribute string.  For example, if fully qualified
     * name is "a:b:c:d", this method will return "d".
     *
     * @param attr
     * @return
     */
    fun name(attr: String): String {
        return attr.substring(attr.lastIndexOf(DELIMITER) + 1)
    }

    /**
     * Return the fully qualified namespace for the attribute.  For example, if
     * the attribute is "a:b:c:d", this method will return "a:b:c"
     *
     * @param attr
     * @return
     */
    fun namespace(attr: String): String {
        return attr.substring(0, attr.lastIndexOf(DELIMITER))
    }
}

object IdGen {

    private val uuidGenerator = Generators.nameBasedGenerator(NameBasedGenerator.NAMESPACE_URL)

    /**
     * Generate an UUID string utilizing the given value.
     *
     * @param value
     * @return
     */
    fun getId(value: String): String {
        return uuidGenerator.generate(value).toString()
    }

    /**
     * Returns a readable unique string for an asset that is based
     * on the file path and clip properties.
     *
     * @param source
     * @return
     */
    fun getRef(source: Document): String {
        val idkey = source.getAttr("source.idkey", String::class.java)
        var key = source.getAttr("source.path", String::class.java)

        if (idkey != null) {
            key = "$key?$idkey"
        }
        return key
    }

    /**
     * Return the unique ID for the given source.
     *
     * @param source
     * @return
     */
    fun getId(source: Document): String {
        return uuidGenerator.generate(getRef(source)).toString()
    }
}

/**
 * Copied from IRM source.
 */
enum class DocumentState private constructor(val value: Long) {
    METADATA_UPLOADED(10), DOCUMENT_UPLOADED(20), PROCESSED(30), INDEXED(40), REPROCESS(50);

    companion object {
        fun findByValue(value: Long): DocumentState? {
            for (state in DocumentState.values()) {
                if (state.value == value) {
                    return state
                }
            }
            return null
        }
    }
}

class Tuple<L, R>(val left: L, val right: R)
class MutableTuple<L, R>(var left: L, var right: R)



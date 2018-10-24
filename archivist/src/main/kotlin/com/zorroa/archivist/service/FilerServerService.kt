package com.zorroa.archivist.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.*
import com.google.common.net.UrlEscapers
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.domain.Document
import org.apache.tika.Tika
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.URI
import java.net.URL
import java.nio.channels.Channels
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import javax.servlet.http.HttpServletResponse

/**
 * The FileServerService system is for serving source files that live in different repositories
 */

private val tika = Tika()

private const val defaultContentType = "application/octet-steam"

data class FileStat(val size: Long, val contentType: String, val exists: Boolean)

/**
 * On object that can be stored somewhere in the vast reaches of the interweb.
 */
class ServableFile (
        private val fileServerService: FileServerService,
        val uri: URI) {

    fun exists() : Boolean {
        return fileServerService.objectExists(uri)
    }

    fun isLocal() : Boolean {
        return fileServerService.storedLocally
    }

    fun getSignedUrl() : URL {
        return fileServerService.getSignedUrl(uri)
    }

    fun getReponseEntity() : ResponseEntity<InputStreamResource> {
        return fileServerService.getReponseEntity(uri)
    }

    fun copyTo(response: HttpServletResponse) {
        return fileServerService.copyTo(uri, response)
    }

    fun getLocalFile() : Path? {
        return fileServerService.getLocalPath(uri)
    }

    fun getStat(): FileStat {
        return fileServerService.getStat(uri)
    }

    /**
     * Currently unsupported
     */
    fun delete() : Boolean {
        return false
    }
}

/**
 * FileServerProvider handles proving a proper FileServerService based on a given URI.
 */
interface FileServerProvider {

    fun getStorageUri(doc: Document) : URI {
        val stream = doc.getAttr("source.stream", String::class.java)

        return if (stream != null) {
            URI(UrlEscapers.urlFragmentEscaper().escape(stream))
        }
        else {
            val path = doc.getAttr("source.path", String::class.java)
            URI("file://$path")
        }
    }

    fun getServableFile(uri: URI): ServableFile
    fun getServableFile(doc: Document): ServableFile
    fun getServableFile(uri: String) : ServableFile = getServableFile(URI(uri))
}

@Component
class FileServerProviderImpl @Autowired constructor (
        val properties: ApplicationProperties) : FileServerProvider {

    private val services : Map<String, FileServerService>

    init {
        services = mutableMapOf()
        val internalStorageType = properties.getString("archivist.storage.type")

        if (internalStorageType== "gcp") {
            services["gcp"] = GcpFileServerService(properties)
        }
        else {
            services["local"] = LocalFileServerService(properties)
        }

    }

    fun getStorageService(uri: URI) : FileServerService {
        val type = when(uri.scheme) {
            "gs" -> "gcp"
            "file" -> "local"
            null->"local"
            else -> uri.scheme
        }

        return services[type] ?: throw FileServerException(
                "Unable to find storage service for: $type")
    }

    override fun getServableFile(uri: URI): ServableFile {
        val service = getStorageService(uri)
        return ServableFile(service, uri)
    }

    override fun getServableFile(doc: Document): ServableFile {
        val uri = getStorageUri(doc)
        return getServableFile(uri)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(FileServerProviderImpl::class.java)
    }
}

/**
 * Implementations of the FileServerService are responsible for serving files to the client,
 * be they internally stored files or external files.
 */
interface FileServerService {

    val storedLocally : Boolean

    fun getReponseEntity(url: URI) : ResponseEntity<InputStreamResource>

    fun copyTo(url: URI, response: HttpServletResponse)

    fun copyTo(url: URI, output: OutputStream)

    fun objectExists(url: URI): Boolean

    fun getSignedUrl(url: URI): URL

    fun getLocalPath(url: URI) : Path?

    fun getStat(url: URI) : FileStat
}

class LocalFileServerService @Autowired constructor (
        val properties: ApplicationProperties) : FileServerService {

    override val storedLocally: Boolean
        get() = true

    init {
        logger.info("Initializing local mount file server")
    }
    override fun getReponseEntity(url: URI): ResponseEntity<InputStreamResource> {
        val path = Paths.get(url)
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tika.detect(path)))
                .contentLength(Files.size(path))
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                .body(InputStreamResource(FileInputStream(path.toFile())))
    }

    override fun copyTo(url: URI, response: HttpServletResponse) {
        val path = Paths.get(url)
        response.setContentLengthLong(Files.size(path))
        response.contentType = tika.detect(path)
        FileInputStream(path.toFile()).copyTo(response.outputStream)
    }

    override fun copyTo(url: URI, output: OutputStream) {
        val path = Paths.get(url)
        FileInputStream(path.toFile()).copyTo(output)
    }

    override fun objectExists(url: URI): Boolean {
        return File(url).exists()
    }

    override fun getSignedUrl(url: URI): URL {
        return url.toURL()
    }

    override fun getLocalPath(url: URI): Path? {
      return Paths.get(url)
    }

    override fun getStat(url: URI): FileStat {
       return try {
           val path = getLocalPath(url)
           FileStat(Files.size(path), tika.detect(path), objectExists(url))
       } catch (e: Exception) {
           FileStat(0, defaultContentType, false)
       }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LocalFileServerService::class.java)
    }
}

class GcpFileServerService constructor (
        val properties: ApplicationProperties) : FileServerService {

    private val storage: Storage
    init {
        logger.info("Initializing Google Cloud Storage file server")

        val configPath = properties.getPath("archivist.config.path").resolve("data-credentials.json")
        storage = if (Files.exists(configPath)) {
            StorageOptions.newBuilder().setCredentials(
                    GoogleCredentials.fromStream(FileInputStream(
                            configPath.resolve("data-credentials.json").toFile()))).build().service
        }
        else {
            StorageOptions.newBuilder().build().service
        }

    }

    override val storedLocally: Boolean
        get() = false

    override fun getReponseEntity(url: URI): ResponseEntity<InputStreamResource> {
        val blob =  getBlob(url)
        if (blob != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(blob.contentType))
                    .contentLength(blob.size)
                    .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                    .body(InputStreamResource(Channels.newInputStream(blob.reader())))
        } else {
            throw FileServerReadException("$url not found")
        }
    }

    override fun copyTo(url: URI, response: HttpServletResponse) {
        val blob =  getBlob(url)
        if (blob != null) {
            response.setContentLengthLong(blob.size)
            response.contentType = blob.contentType
            Channels.newInputStream(blob.reader()).copyTo(response.outputStream)
        } else {
            throw FileServerReadException("$url not found")
        }
    }

    override fun copyTo(url: URI, output: OutputStream) {
        val blob =  getBlob(url)
        if (blob != null) {
            Channels.newInputStream(blob.reader()).copyTo(output)
        } else {
            throw FileServerReadException("$url not found")
        }
    }

    override fun getSignedUrl(url: URI): URL {
        val blob = getBlob(url)
        if (blob != null) {
            return blob.signUrl(60, TimeUnit.MINUTES,
                    Storage.SignUrlOption.httpMethod(HttpMethod.GET))
        }
        else {
            throw FileServerReadException("$url not found")
        }
    }

    override fun objectExists(url: URI): Boolean {
        var (bucket, path) =  splitGcpUrl(url)
        val blobId = BlobId.of(bucket, path)
        val storage = storage.get(blobId) ?: return false
        return storage.exists()
    }

    private fun getBlob(url: URI) : Blob? {
        var (bucket, path) =  splitGcpUrl(url)
        val blobId = BlobId.of(bucket, path)
        return storage.get(blobId)
    }

    private fun splitGcpUrl(url: URI) : Array<String> {
        return arrayOf (
            url.authority,
            url.path
        )
    }

    override fun getLocalPath(url: URI): Path? {
       return null
    }

    override fun getStat(url: URI): FileStat {
        val blob = getBlob(url)
        return if (blob != null) {
            FileStat(blob.size, blob.contentType, objectExists(url))
        }
        else {
            FileStat(0, defaultContentType, false)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GcpFileServerService::class.java)
    }
}

open class FileServerException(override var message:String?) : RuntimeException(message) {
    constructor(e: Exception) : this(e.message) {
        this.initCause(e)
    }
}

class FileServerReadException (override var message:String?) : FileServerException(message)


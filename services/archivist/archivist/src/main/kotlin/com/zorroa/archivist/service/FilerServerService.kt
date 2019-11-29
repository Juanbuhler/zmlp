package com.zorroa.archivist.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.HttpMethod
import com.google.cloud.storage.Storage
import com.google.cloud.storage.StorageOptions
import com.zorroa.archivist.config.ApplicationProperties
import com.zorroa.archivist.config.SpringApplicationProperties
import com.zorroa.archivist.domain.FileStat
import com.zorroa.archivist.domain.LogAction
import com.zorroa.archivist.domain.LogObject
import com.zorroa.archivist.util.StaticUtils
import com.zorroa.archivist.util.copyInputToOuput
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.FileSystemUtils
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
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

private const val defaultContentType = "application/octet-steam"

private const val entity = "FileStorage"

/**
 * Implementations of the FileServerService are responsible for serving files to the client,
 * be they internally stored files or external files.
 */

@Service
interface FileServerService {

    val storedLocally: Boolean

    fun getReponseEntity(url: URI): ResponseEntity<InputStreamResource>

    fun copyTo(url: URI, response: HttpServletResponse)

    fun copyTo(url: URI, output: OutputStream)

    fun getInputStream(url: URI): InputStream

    fun objectExists(url: URI): Boolean

    fun getSignedUrl(url: URI): URL

    fun getLocalPath(url: URI): Path?

    fun getStat(url: URI): FileStat

    fun delete(url: URI): Boolean
}

@Profile("local")
@Service
class LocalFileServerService : FileServerService {

    override val storedLocally: Boolean
        get() = true

    init {
        logger.info("Initializing local mount file server")
    }

    override fun getReponseEntity(url: URI): ResponseEntity<InputStreamResource> {
        val path = Paths.get(url)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(StaticUtils.tika.detect(path)))
            .contentLength(Files.size(path))
            .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
            .body(InputStreamResource(FileInputStream(path.toFile())))
    }

    override fun getInputStream(url: URI): InputStream {
        val path = Paths.get(url)
        return FileInputStream(path.toFile())
    }

    override fun copyTo(url: URI, response: HttpServletResponse) {
        val path = Paths.get(url)
        response.setContentLengthLong(Files.size(path))
        response.contentType = StaticUtils.tika.detect(path)
        copyInputToOuput(FileInputStream(path.toFile()), response.outputStream)
    }

    override fun copyTo(url: URI, output: OutputStream) {
        val path = Paths.get(url)
        copyInputToOuput(FileInputStream(path.toFile()), output)
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
        val path = getLocalPath(url)

        return try {
            val path = getLocalPath(url)
            FileStat(Files.size(path), StaticUtils.tika.detect(path), objectExists(url))
        } catch (e: Exception) {
            // guessing mimeType from string path
            FileStat(0, StaticUtils.tika.detect(path.toString()), false)
        }
    }

    override fun delete(uri: URI): Boolean {
        logger.event(LogObject.STORAGE, LogAction.DELETE, mapOf("uri" to uri.toString()))
        val path = Paths.get(uri)
        return if (path.toFile().isDirectory) {
            FileSystemUtils.deleteRecursively(path)
        } else {
            Files.deleteIfExists(path)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LocalFileServerService::class.java)
    }
}

@Profile("gcs")
@Service
class GcpFileServerService @Autowired constructor
    (val properties: ApplicationProperties) : FileServerService {

    lateinit var credentials: Path

    lateinit var storage: Storage

    init {
        logger.info("Initializing Google Cloud Storage file server")

        loadStorage(
            properties
                .getPath("archivist.config.path")
                .resolve("data-credentials.json")
        )
    }

    constructor(path: Path) : this(SpringApplicationProperties()) {
        loadStorage(path)
    }

    fun loadStorage(path: Path) {

        this.credentials = path

        this.storage =
            if (credentials != null && Files.exists(credentials)) {
                StorageOptions.newBuilder().setCredentials(
                    GoogleCredentials.fromStream(FileInputStream(credentials.toFile()))
                ).build().service
            } else {
                StorageOptions.newBuilder().build().service
            }
    }

    override val storedLocally: Boolean
        get() = false

    override fun getReponseEntity(url: URI): ResponseEntity<InputStreamResource> {
        val blob = getBlob(url)
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
        val blob = getBlob(url)
        if (blob != null) {
            response.setContentLengthLong(blob.size)
            response.contentType = blob.contentType
            Channels.newInputStream(blob.reader()).copyTo(response.outputStream)
        } else {
            throw FileServerReadException("$url not found")
        }
    }

    override fun copyTo(url: URI, output: OutputStream) {
        val blob = getBlob(url)
        if (blob != null) {
            Channels.newInputStream(blob.reader()).copyTo(output)
        } else {
            throw FileServerReadException("$url not found")
        }
    }

    override fun getInputStream(url: URI): InputStream {
        val blob = getBlob(url)
        if (blob != null) {
            return Channels.newInputStream(blob.reader())
        } else {
            throw FileServerReadException("$url not found")
        }
    }

    override fun getSignedUrl(url: URI): URL {
        val blob = getBlob(url)
        if (blob != null) {
            logger.event(LogObject.STORAGE, LogAction.AUTHORIZE, mapOf("uri" to url.toString()))
            return blob.signUrl(
                60, TimeUnit.MINUTES,
                Storage.SignUrlOption.httpMethod(HttpMethod.GET)
            )
        } else {
            throw FileServerReadException("$url not found")
        }
    }

    override fun objectExists(url: URI): Boolean {
        var (bucket, path) = splitGcpUrl(url)
        val blobId = BlobId.of(bucket, path)
        val storage = storage.get(blobId) ?: return false
        return storage.exists()
    }

    private fun getBlob(uri: URI): Blob? {
        var (bucket, path) = splitGcpUrl(uri)
        val blobId = BlobId.of(bucket, path)
        logger.event(LogObject.STORAGE, LogAction.GET, mapOf("uri" to uri.toString()))
        return storage.get(blobId)
    }

    private fun splitGcpUrl(url: URI): Array<String> {
        return arrayOf(
            url.authority,
            url.path.removePrefix("/")
        )
    }

    override fun getLocalPath(url: URI): Path? {
        return null
    }

    override fun getStat(url: URI): FileStat {
        val blob = getBlob(url)
        return if (blob != null) {
            FileStat(blob.size, blob.contentType, objectExists(url))
        } else {
            FileStat(0, defaultContentType, false)
        }
    }

    override fun delete(url: URI): Boolean {
        var result = true
        var (bucket, path) = splitGcpUrl(url)
        val blobs = storage.list(
            bucket, Storage.BlobListOption.pageSize(100),
            Storage.BlobListOption.prefix(path)
        )

        for (blob in blobs.iterateAll()) {
            if (blob.delete()) {
                logger.event(LogObject.STORAGE, LogAction.DELETE, mapOf("url" to url.toString()))
            } else {
                logger.warnEvent(
                    LogObject.STORAGE, LogAction.DELETE, "Did not exist",
                    mapOf("url" to url.toString())
                )
                result = false
            }
        }
        return result
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GcpFileServerService::class.java)
    }
}

open class FileServerException(override var message: String?) : RuntimeException(message) {
    constructor(e: Exception) : this(e.message) {
        this.initCause(e)
    }
}

class FileServerReadException(override var message: String?) : FileServerException(message)

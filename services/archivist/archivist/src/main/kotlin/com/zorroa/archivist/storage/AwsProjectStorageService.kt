package com.zorroa.archivist.storage

import com.amazonaws.ClientConfiguration
import com.amazonaws.HttpMethod
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.AmazonS3Exception
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.google.cloud.storage.StorageException
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.ProjectDirLocator
import com.zorroa.archivist.domain.ProjectStorageLocator
import com.zorroa.archivist.domain.ProjectStorageSpec
import com.zorroa.archivist.service.IndexRoutingService
import com.zorroa.archivist.util.FileUtils
import com.zorroa.zmlp.service.logging.LogAction
import com.zorroa.zmlp.service.logging.LogObject
import com.zorroa.zmlp.service.logging.warnEvent
import com.zorroa.zmlp.util.Json
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Configuration
@Profile("aws")
class AwsStorageConfiguration(val properties: StorageProperties) {

    @Bean
    fun getS3Client(): AmazonS3 {

        val credentials: AWSCredentials = BasicAWSCredentials(
            properties.accessKey, properties.secretKey
        )

        val clientConfiguration = ClientConfiguration()
        clientConfiguration.signerOverride = "AWSS3V4SignerType"

        return AmazonS3ClientBuilder
            .standard()
            .withEndpointConfiguration(
                EndpointConfiguration(
                    properties.url, Regions.DEFAULT_REGION.name
                )
            )
            .withPathStyleAccessEnabled(true)
            .withClientConfiguration(clientConfiguration)
            .withCredentials(AWSStaticCredentialsProvider(credentials))
            .build()
    }
}

@Service
@Profile("aws")
class AwsProjectStorageService constructor(
    val properties: StorageProperties,
    val indexRoutingService: IndexRoutingService,
    val s3Client: AmazonS3
) : ProjectStorageService {

    @PostConstruct
    fun initialize() {
        logger.info("Initializing AWS Storage Backend (bucket='${properties.bucket}')")
        if (properties.createBucket) {
            if (!s3Client.doesBucketExistV2(properties.bucket)) {
                s3Client.createBucket(properties.bucket)
            }
        }
    }

    override fun store(spec: ProjectStorageSpec): FileStorage {

        val path = spec.locator.getPath()
        val metadata = ObjectMetadata()
        metadata.contentType = spec.mimetype
        metadata.contentLength = spec.data.size.toLong()
        metadata.userMetadata = mapOf("attrs" to Json.serializeToString(spec.attrs))

        s3Client.putObject(
            PutObjectRequest(
                properties.bucket, path,
                spec.data.inputStream(), metadata
            )
        )

        logStoreEvent(spec)

        return FileStorage(
            spec.locator.getFileId(),
            spec.locator.name,
            spec.locator.category,
            spec.mimetype,
            spec.data.size.toLong(),
            spec.attrs
        )
    }

    override fun stream(locator: ProjectStorageLocator): ResponseEntity<Resource> {
        val path = locator.getPath()
        val s3obj = s3Client.getObject(GetObjectRequest(properties.bucket, path))

        return try {
            ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(s3obj.objectMetadata.contentType))
                .contentLength(s3obj.objectMetadata.contentLength)
                .cacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePrivate())
                .body(InputStreamResource(s3obj.objectContent))
        } catch (e: StorageException) {
            ResponseEntity.noContent().build()
        }
    }

    override fun fetch(locator: ProjectStorageLocator): ByteArray {
        val path = locator.getPath()
        try {
            val s3obj = s3Client.getObject(GetObjectRequest(properties.bucket, path))
            return s3obj.objectContent.readBytes()
        } catch (ex: AmazonS3Exception) {
            throw ProjectStorageException("Failed to fetch $path", ex)
        }
    }

    override fun getNativeUri(locator: ProjectStorageLocator): String {
        return "s3://${properties.bucket}/${locator.getPath()}"
    }

    override fun getSignedUrl(
        locator: ProjectStorageLocator,
        forWrite: Boolean,
        duration: Long,
        unit: TimeUnit
    ): Map<String, Any> {
        val path = locator.getPath()
        val mediaType = FileUtils.getMediaType(path)
        val expireTime = Date(System.currentTimeMillis() + unit.toMillis(duration))
        val method = if (forWrite) {
            HttpMethod.PUT
        } else {
            HttpMethod.GET
        }

        val req: GeneratePresignedUrlRequest =
            GeneratePresignedUrlRequest(properties.bucket, path)
                .withMethod(method)
                .withExpiration(expireTime)

        logSignEvent(path, mediaType, forWrite)
        return mapOf("uri" to
            s3Client.generatePresignedUrl(req).toString(),
            "mediaType" to mediaType)
    }

    override fun setAttrs(locator: ProjectStorageLocator, attrs: Map<String, Any>): FileStorage {

        val path = locator.getPath()
        val metadata = ObjectMetadata()
        metadata.userMetadata = mapOf("attrs" to Json.serializeToString(attrs))

        val obj = s3Client.getObject(properties.bucket, path)

        return FileStorage(
            locator.getFileId(),
            locator.name,
            locator.category,
            obj.objectMetadata.contentType,
            obj.objectMetadata.contentLength,
            attrs
        )
    }

    override fun recursiveDelete(locator: ProjectDirLocator) {
        val path = locator.getPath()
        logger.info("Recursive delete path:${properties.bucket}/$path")

        try {
            s3Client.listObjects(properties.bucket, path).objectSummaries.forEach {
                s3Client.deleteObject(properties.bucket, it.key)
                logDeleteEvent("${properties.bucket}${it.key}")
            }
        } catch (ex: AmazonS3Exception) {
            logger.warnEvent(LogObject.PROJECT_STORAGE, LogAction.DELETE,
                "Failed to delete ${ex.message}",
                mapOf("entityId" to locator.entityId, "entity" to locator.entity))
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(AwsProjectStorageService::class.java)
    }
}

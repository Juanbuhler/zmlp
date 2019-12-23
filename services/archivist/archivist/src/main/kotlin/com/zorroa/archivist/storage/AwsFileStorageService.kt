package com.zorroa.archivist.storage

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.google.cloud.storage.StorageException
import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageLocator
import com.zorroa.archivist.domain.FileStorageSpec
import com.zorroa.archivist.service.IndexRoutingService
import com.zorroa.archivist.util.Json
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

@Configuration
@Profile("aws")
class AwsFileStorageServiceConfiguration(val properties: StorageProperties) {

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
class AwsFileStorageServiceImpl constructor(
    val properties: StorageProperties,
    val indexRoutingService: IndexRoutingService,
    val s3Client: AmazonS3
) : FileStorageService {

    @PostConstruct
    fun initialize() {
        FileStorageService.logger.info("Initializing AWS Storage Backend (bucket='${properties.bucket}')")
        if (properties.createBucket) {
            if (!s3Client.doesBucketExistV2(properties.bucket)) {
                s3Client.createBucket(properties.bucket)
            }
        }
    }

    override fun store(spec: FileStorageSpec): FileStorage {

        val path = spec.locator.getPath()
        val metadata = ObjectMetadata()
        metadata.contentType = spec.mimetype
        metadata.contentLength = spec.data.size.toLong()
        metadata.userMetadata = mapOf("attrs" to Json.serializeToString(spec.attrs))

        s3Client.putObject(PutObjectRequest(properties.bucket, path,
           spec.data.inputStream(), metadata))

        logStoreEvent(spec)

        return FileStorage(
            spec.locator.name,
            spec.locator.category.toString().toLowerCase(),
            spec.mimetype,
            spec.data.size.toLong(),
            spec.attrs
        )
    }

    override fun stream(locator: FileStorageLocator): ResponseEntity<Resource> {
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

    override fun fetch(locator: FileStorageLocator): ByteArray {
        val path = locator.getPath()
        val s3obj = s3Client.getObject(GetObjectRequest(properties.bucket, path))
        return s3obj.objectContent.readAllBytes()
    }
}

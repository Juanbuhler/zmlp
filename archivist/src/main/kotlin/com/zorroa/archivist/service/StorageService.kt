package com.zorroa.archivist.service

import com.zorroa.archivist.sdk.config.ApplicationProperties
import com.zorroa.archivist.sdk.services.*
import com.zorroa.sdk.util.Json
import io.minio.MinioClient
import io.minio.errors.MinioException
import org.apache.commons.io.IOUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream

@Service
class MinioStorageImpl @Autowired constructor (
        val properties: ApplicationProperties) : StorageService {

    private val client: MinioClient = MinioClient(
            properties.getString("archivist.storage.minio.endpoint"),
            properties.getString("archivist.storage.minio.accessKey"),
            properties.getString("archivist.storage.minio.secretKey"))

    override fun removeBucket(asset: AssetId) {
        try {
            client.removeBucket(getBucketName(asset))
        } catch (e: MinioException) {
            throw StorageWriteException(e)
        }
    }

    override fun bucketExists(asset: AssetId): Boolean {
        try {
            return client.bucketExists(getBucketName(asset))
        } catch (e: MinioException) {
            throw StorageReadException(e)
        }
    }

    override fun createBucket(asset: AssetId) {
        val name = getBucketName(asset)
        try {
            if (!client.bucketExists(name)) {
                client.makeBucket(name)
            }
        } catch (e: MinioException) {
            throw StorageWriteException(e)
        }
    }

    override fun storeMetadata(asset: AssetId, metadata: Map<String, Any>?) {
        val md = metadata ?: mapOf()
        client.putObject(getBucketName(asset), getFileName(asset, "metadata.json"),
                ByteArrayInputStream(Json.Mapper.writeValueAsString(md).toByteArray(Charsets.UTF_8)),
                "application/octet-stream")
    }

    override fun storeSourceFile(asset: AssetId, stream: InputStream) {
        client.putObject(getBucketName(asset), getFileName(asset), stream,
                "application/octet-stream")
    }

    override fun getMetadata(asset: AssetId): Map<String, Any> {
        val stream = client.getObject(getBucketName(asset),
                getFileName(asset, "metadata.json"))
        return Json.Mapper.readValue(stream, Json.GENERIC_MAP)
    }

    override fun getSourceFile(asset: AssetId): InputStream {
        return client.getObject(getBucketName(asset), getFileName(asset))
    }

    override fun streamSourceFile(asset: AssetId, output: OutputStream) {
        IOUtils.copy(client.getObject(getBucketName(asset), getFileName(asset)), output)
    }

    override fun storeFile(asset: AssetId, name: String, stream: InputStream) {
        client.putObject(getBucketName(asset), getFileName(asset, name), stream,
                "application/octet-stream")
    }

    override fun getFile(asset: AssetId, name: String): InputStream {
        return client.getObject(getBucketName(asset), getFileName(asset, name))
    }

    override fun streamFile(asset: AssetId, name: String, output: OutputStream) {
        IOUtils.copy(client.getObject(getBucketName(asset), getFileName(asset, name)), output)
    }

    companion object {

        private val logger = LoggerFactory.getLogger(MinioStorageImpl::class.java)

    }
}

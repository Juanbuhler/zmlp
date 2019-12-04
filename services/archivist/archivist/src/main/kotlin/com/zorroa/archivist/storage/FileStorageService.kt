package com.zorroa.archivist.storage

import com.zorroa.archivist.domain.FileStorage
import com.zorroa.archivist.domain.FileStorageLocator
import com.zorroa.archivist.domain.FileStorageSpec
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity

@Configuration
@ConfigurationProperties("archivist.storage")
class StorageProperties {

    lateinit var bucket: String

    var createBucket: Boolean = false

    var accessKey: String? = null

    var secretKey: String? = null

    var url: String? = null
}

interface FileStorageService {

    /**
     * Store the file described by the [FileStorageSpec] in bucket storage.
     */
    fun store(spec: FileStorageSpec): FileStorage

    /**
     * Stream the given file as a ResponseEntity.
     */
    fun stream(locator: FileStorageLocator): ResponseEntity<Resource>

    /**
     * Fetch the bytes for the given file.
     */
    fun fetch(locator: FileStorageLocator): ByteArray
}

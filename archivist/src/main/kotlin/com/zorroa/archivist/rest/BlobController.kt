package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Access
import com.zorroa.archivist.domain.Acl
import com.zorroa.archivist.domain.Blob
import com.zorroa.archivist.domain.BlobSpec
import com.zorroa.archivist.domain.SetPermissions
import com.zorroa.archivist.util.HttpUtils
import com.zorroa.archivist.service.BlobService
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Timed
class BlobController @Autowired constructor(
    private val blobService: BlobService
) {

    @PostMapping(value = ["/api/v1/blobs/{app}/{feature}/{name}"])
    fun set(@RequestBody data: Any, @PathVariable app: String, @PathVariable feature: String, @PathVariable name: String): Blob {
        return blobService.set(app, feature, name, data)
    }

    @PostMapping(value = ["/api/v2/blobs/{app}/{feature}/{name}"])
    fun setV2(@RequestBody spec: BlobSpec, @PathVariable app: String, @PathVariable feature: String, @PathVariable name: String): Blob {
        return blobService.set(app, feature, name, spec)
    }

    @GetMapping(value = ["/api/v1/blobs/{app}/{feature}/{name}"])
    operator fun get(@PathVariable app: String, @PathVariable feature: String, @PathVariable name: String): Blob {
        return blobService.get(app, feature, name)
    }

    @DeleteMapping(value = ["/api/v1/blobs/{app}/{feature}/{name}"])
    fun delete(@PathVariable app: String, @PathVariable feature: String, @PathVariable name: String): Any {
        val blob = blobService.getId(app, feature, name, Access.Write)
        return HttpUtils.deleted("blob", blob.getBlobId(), blobService.delete(blob))
    }

    /**
     * Gets just the data structure without any of the other properties.
     *
     * @param app
     * @param feature
     * @param name
     * @return
     */
    @GetMapping(value = ["/api/v1/blobs/{app}/{feature}/{name}/_raw"])
    fun getRaw(@PathVariable app: String, @PathVariable feature: String, @PathVariable name: String): Any? {
        return blobService.get(app, feature, name).data
    }

    @GetMapping(value = ["/api/v1/blobs/{app}/{feature}"])
    fun getAll(@PathVariable app: String, @PathVariable feature: String): List<Blob> {
        return blobService.getAll(app, feature)
    }

    @PutMapping(value = ["/api/v1/blobs/{app}/{feature}/{name}/_permissions"])
    fun setPermissions(@RequestBody req: SetPermissions, @PathVariable app: String, @PathVariable feature: String, @PathVariable name: String): Acl {
        val blob = blobService.getId(app, feature, name, Access.Write)
        return blobService.setPermissions(blob, req)
    }

    @GetMapping(value = ["/api/v1/blobs/{app}/{feature}/{name}/_permissions"])
    fun getPermisions(@PathVariable app: String, @PathVariable feature: String, @PathVariable name: String): Acl {
        val blob = blobService.getId(app, feature, name, Access.Read)
        return blobService.getPermissions(blob)
    }
}

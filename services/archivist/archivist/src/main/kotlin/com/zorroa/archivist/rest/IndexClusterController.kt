package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.IndexCluster
import com.zorroa.archivist.domain.IndexClusterFilter
import com.zorroa.archivist.domain.IndexClusterSpec
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.service.IndexClusterService
import io.micrometer.core.annotation.Timed
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@PreAuthorize("hasAuthority('SuperAdmin')")
@RestController
@Timed
@Api(tags = ["Index Cluster"], description = "Operations for managing Index Clusters.")
class IndexClusterController constructor(
    val indexClusterService: IndexClusterService
) {

    @PostMapping(value = ["/api/v1/index-clusters"])
    @ApiOperation("Create Project.")
    fun create(@RequestBody spec: IndexClusterSpec): IndexCluster {
        return indexClusterService.createIndexCluster(spec)
    }

    @GetMapping(value = ["/api/v1/index-clusters/{id}"])
    @ApiOperation("Find Project by Id")
    fun get(@PathVariable id: UUID): IndexCluster {
        return indexClusterService.getIndexCluster(id)
    }

    @GetMapping(value = ["/api/v1/index-clusters/_search"])
    @ApiOperation("Search Filter.")
    fun search(@RequestBody(required = false) filter: IndexClusterFilter?): KPagedList<IndexCluster> {
        return indexClusterService.getAll(filter ?: IndexClusterFilter())
    }
}
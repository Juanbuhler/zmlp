package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.Project
import com.zorroa.archivist.domain.ProjectFilter
import com.zorroa.archivist.domain.ProjectQuotas
import com.zorroa.archivist.domain.ProjectQuotasTimeSeriesEntry
import com.zorroa.archivist.domain.ProjectSettings
import com.zorroa.archivist.domain.ProjectSpec
import com.zorroa.archivist.domain.ProjectTierUpdate
import com.zorroa.archivist.repository.KPagedList
import com.zorroa.archivist.security.getProjectId
import com.zorroa.archivist.service.ProjectService
import com.zorroa.archivist.storage.ProjectStorageService
import com.zorroa.archivist.util.HttpUtils
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Date
import java.util.UUID

/**
 * The /api/v1/projects endpoints (plural) are mainly admin endpoints that
 * can see multiple projects.
 *
 * The /api/v1/project endpoints utilize the authed API Key's project.
 *
 */
@RestController
@Api(tags = ["Project"], description = "Operations for managing Projects.")
class ProjectController constructor(
    val projectService: ProjectService,
    val projectStorageService: ProjectStorageService
) {

    @PreAuthorize("hasAuthority('SystemManage')")
    @PostMapping(value = ["/api/v1/projects"])
    @ApiOperation("Create Project.")
    fun create(@RequestBody spec: ProjectSpec): Project {
        return projectService.create(spec)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @GetMapping(value = ["/api/v1/projects/{id}"])
    @ApiOperation("Retrieve Project by Id.")
    fun get(@PathVariable id: UUID): Project {
        return projectService.get(id)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @RequestMapping(value = ["/api/v1/projects/_search"], method = [RequestMethod.POST, RequestMethod.GET])
    @ApiOperation("Get all Projects")
    fun getAll(@RequestBody(required = false) filter: ProjectFilter?): KPagedList<Project> {
        return projectService.getAll(filter ?: ProjectFilter())
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @RequestMapping(value = ["/api/v1/projects/_findOne"], method = [RequestMethod.POST, RequestMethod.GET])
    @ApiOperation("Search for a single project")
    fun findOne(@RequestBody(required = false) filter: ProjectFilter?): Project {
        return projectService.findOne(filter ?: ProjectFilter())
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @GetMapping(value = ["/api/v1/projects/{id}/_settings"])
    @ApiOperation("Get the project Settings")
    fun getSettings(@PathVariable id: UUID): ProjectSettings {
        return projectService.getSettings(id)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @PutMapping(value = ["/api/v1/projects/{id}/_settings"])
    @ApiOperation("Get the project Settings")
    fun putSettings(@PathVariable id: UUID, @RequestBody(required = true) settings: ProjectSettings): ProjectSettings {
        projectService.updateSettings(id, settings)
        return projectService.getSettings(id)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @PutMapping(value = ["/api/v1/project/{id}/_enable"])
    @ApiOperation("Set an disabled project to enabled.")
    fun putEnabled(@PathVariable id: UUID): Any {
        projectService.setEnabled(id, true)
        return HttpUtils.status("project", id.toString(), "enable", true)
    }

    @PreAuthorize("hasAuthority('SystemManage')")
    @PutMapping(value = ["/api/v1/project/{id}/_disable"])
    @ApiOperation("Set a disabled project to enabled.")
    fun putDisabled(@PathVariable id: UUID): Any {
        projectService.setEnabled(id, false)
        return HttpUtils.status("project", id.toString(), "disable", true)
    }

    //
    // Methods that default to the API Keys project Id.
    //
    @GetMapping(value = ["/api/v1/project"])
    @ApiOperation("Retrieve my current project.")
    fun getMyProject(): Project {
        return projectService.get(getProjectId())
    }

    @PreAuthorize("hasAuthority('ProjectManage')")
    @GetMapping(value = ["/api/v1/project/_quotas"])
    @ApiOperation("Retrieve my current project quotas")
    fun getMyProjectQuotas(): ProjectQuotas {
        return projectService.getQuotas(getProjectId())
    }

    @PreAuthorize("hasAuthority('ProjectManage')")
    @GetMapping(value = ["/api/v1/project/_quotas_time_series"])
    @ApiOperation("Retrieve time serious measurements of quota counters")
    fun getMyProjectQuotasTimeSeries(
        @RequestParam("start", required = false) start: Long?,
        @RequestParam("stop", required = false) stop: Long?
    ): List<ProjectQuotasTimeSeriesEntry> {
        return projectService.getQuotasTimeSeries(getProjectId(),
            Date(start ?: System.currentTimeMillis() - 86400000L),
            Date(stop ?: System.currentTimeMillis()))
    }

    @PreAuthorize("hasAuthority('ProjectManage')")
    @GetMapping(value = ["/api/v1/project/_settings"])
    @ApiOperation("Retrieve my current project.")
    fun getMyProjectSettings(): ProjectSettings {
        return projectService.getSettings(getProjectId())
    }

    @PreAuthorize("hasAuthority('ProjectManage')")
    @PutMapping(value = ["/api/v1/project/_settings"])
    @ApiOperation("Get the project Settings")
    fun putMyProjectSettings(@RequestBody(required = true) settings: ProjectSettings):
        ProjectSettings {
        val id = getProjectId()
        projectService.updateSettings(id, settings)
        return projectService.getSettings(id)
    }

    @PreAuthorize("hasAuthority('ProjectManage')")
    @PutMapping(value = ["/api/v1/project/{id}/_update_tier"])
    @ApiOperation("Update Project Tier")
    fun updateProjectTier(@PathVariable id: UUID, @RequestBody(required = true) projectTierUpdate: ProjectTierUpdate):
        Project{
        projectService.setTier(id, projectTierUpdate.tier)
        return projectService.get(id)
    }


}

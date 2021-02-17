package boonai.archivist.rest

import boonai.archivist.domain.Field
import boonai.archivist.domain.FieldSpec
import boonai.archivist.service.FieldService
import boonai.archivist.service.IndexRoutingService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class FieldController(
    val indexRoutingService: IndexRoutingService,
    val fieldService: FieldService
) {

    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping(value = ["/api/v3/fields/_mapping"])
    fun mapping(): Map<String, Any> {
        return indexRoutingService.getProjectRestClient().getMapping()
    }

    @PreAuthorize("hasAuthority('AssetsImport')")
    @PostMapping(value = ["/api/v3/custom-fields"])
    fun createField(@RequestBody spec: FieldSpec): Field {
        return fieldService.createField(spec)
    }

    @PreAuthorize("hasAuthority('AssetsRead')")
    @GetMapping(value = ["/api/v3/custom-fields/{id}"])
    fun getField(@PathVariable id: UUID): Field {
        return fieldService.getField(id)
    }
}
package com.zorroa.archivist.rest

import com.zorroa.archivist.domain.AuditLogEntry
import com.zorroa.archivist.domain.AuditLogFilter
import com.zorroa.archivist.repository.AuditLogDao
import com.zorroa.common.repository.KPagedList
import io.micrometer.core.annotation.Timed
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@Timed
class AuditLogController @Autowired constructor(val auditLogDao: AuditLogDao) {

    @PostMapping(value= ["/api/v1/auditlog/_search"])
    fun getAll(@RequestBody filter: AuditLogFilter) : KPagedList<AuditLogEntry> {
        return auditLogDao.getAll(filter)
    }

    @PostMapping(value = ["/api/v1/auditlog/_findOne"])
    fun findOne(@RequestBody filter: AuditLogFilter): AuditLogEntry {
        return auditLogDao.findOne(filter)
    }
}

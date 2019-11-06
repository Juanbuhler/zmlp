package com.zorroa.auth.rest

import com.zorroa.auth.domain.ApiKey
import com.zorroa.auth.domain.ZmlpUser
import io.swagger.annotations.ApiOperation
import org.springframework.http.HttpHeaders
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController {

    @ApiOperation("Authenticate a signed JWT token and return the projectId and permissions.")
    @RequestMapping("/auth/v1/auth-token", method = [RequestMethod.GET, RequestMethod.POST])
    fun authToken(@RequestHeader headers: HttpHeaders, auth: Authentication): Map<String, Any> {
        val user = auth.principal as ZmlpUser
        return mapOf(
                "name" to user.name,
                "projectId" to user.projectId,
                "keyId" to user.keyId,
                "permissions" to auth.authorities.map { it.toString() })
    }
}
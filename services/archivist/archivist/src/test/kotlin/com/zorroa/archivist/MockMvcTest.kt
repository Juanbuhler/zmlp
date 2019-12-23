package com.zorroa.archivist

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import com.zorroa.archivist.clients.ZmlpActor
import com.zorroa.archivist.security.AnalystAuthentication
import com.zorroa.archivist.security.AnalystTokenValidator
import com.zorroa.archivist.security.Perm
import com.zorroa.archivist.security.Role
import com.zorroa.archivist.util.Json
import org.junit.Before
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.FilterChainProxy
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.io.IOException
import java.util.UUID

abstract class MockMvcTest : AbstractTest() {

    @Autowired
    lateinit var wac: WebApplicationContext

    @Autowired
    lateinit var springSecurityFilterChain: FilterChainProxy

    @MockBean
    lateinit var analystTokenValidator: AnalystTokenValidator

    lateinit var mvc: MockMvc

    @Before
    @Throws(IOException::class)
    override fun setup() {
        super.setup()
        mvc = MockMvcBuilders
            .webAppContextSetup(wac)
            .addFilters<DefaultMockMvcBuilder>(springSecurityFilterChain)
            .build()

        /**
         * When using the 'job()' method to authenticate in a controller test,
         * this will be your PixmlActor.
         */
        whenever(authServerClient.authenticate(eq("JOBRUNNER"))).then {
            ZmlpActor(
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                project.id,
                "JobRunner",
                listOf(Role.JOBRUNNER)
            )
        }

        /**
         * When using the 'admin()' method to authenticate in a controller test,
         * this will be your PixmlActor.
         */
        whenever(authServerClient.authenticate(eq("ADMIN"))).then {
            ZmlpActor(
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                project.id,
                "unittest-key",
                listOf(Role.SUPERADMIN, Role.PROJADMIN, Perm.MONITOR_SERVER)
            )
        }

        whenever(analystTokenValidator.validateJwtToken(eq("ANALYST"), any())).then {
            AnalystAuthentication("http://localhost:5000", "unittest")
        }
    }

    /**
     * Assert successful post to API endpoint
     * @return an an object of the
     */
    final inline fun <reified T : Any> resultForPostContent(
        urlTemplate: String,
        `object`: Any,
        session: HttpHeaders = admin()
    ): T {
        // MockMvc clears the security context when it returns, I don't know how to configure it otherwise.
        val savedAuthentication = SecurityContextHolder.getContext().authentication
        val result = this.mvc.perform(
            MockMvcRequestBuilders
                .post(urlTemplate)
                .headers(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(`object`))
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andReturn()
        SecurityContextHolder.getContext().authentication = savedAuthentication
        val mapper = ObjectMapper().registerKotlinModule()
        return mapper.readValue(result.response.contentAsString)
    }

    fun assertClientErrorForPostContent(
        urlTemplate: String,
        `object`: Any,
        session: HttpHeaders = admin()
    ) {
        val savedAuthentication = SecurityContextHolder.getContext().authentication
        this.mvc.perform(
            MockMvcRequestBuilders
                .post(urlTemplate)
                .headers(session)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(Json.serialize(`object`))
        )
            .andExpect(MockMvcResultMatchers.status().is4xxClientError)
            .andReturn()
        SecurityContextHolder.getContext().authentication = savedAuthentication
    }

    protected fun <T> deserialize(result: MvcResult, type: Class<T>): T {
        return Json.deserialize(result.response.contentAsByteArray, type)
    }

    protected fun <T> deserialize(result: MvcResult, type: TypeReference<T>): T {
        return Json.deserialize(result.response.contentAsByteArray, type)
    }

    protected fun admin(): HttpHeaders {
        val headers = HttpHeaders()
        headers["Authorization"] = "Bearer ADMIN"
        return headers
    }

    protected fun job(): HttpHeaders {
        val headers = HttpHeaders()
        headers["Authorization"] = "Bearer JOBRUNNER"
        return headers
    }

    protected fun analyst(): HttpHeaders {
        val headers = HttpHeaders()
        headers["Authorization"] = "Bearer ANALYST"
        return headers
    }

}

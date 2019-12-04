package com.zorroa.archivist.security

import com.zorroa.archivist.clients.AuthServerClient
import com.zorroa.archivist.clients.AuthServerClientImpl
import com.zorroa.archivist.config.ApplicationProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

/**
 * MultipleWebSecurityConfig sets up all the endpoint security.
 *
 * The secret to doing this is that the configure method in each WebSecurityConfigurerAdapter
 * must start off with a .antMatcher(pattern) function.  Each WebSecurityConfigurerAdapter
 * instance handles configuring a different groups of endpoints.
 *
 * Warning: using this setting messes up tests.
 * SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL)
 */
@EnableWebSecurity
@Order(Ordered.HIGHEST_PRECEDENCE)
class MultipleWebSecurityConfig {

    @Autowired
    internal lateinit var properties: ApplicationProperties

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE+1)
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    class WebSecurityConfig : WebSecurityConfigurerAdapter() {

        @Autowired
        internal lateinit var properties: ApplicationProperties

        @Autowired
        lateinit var apiKeyAuthorizationFilter: ApiKeyAuthorizationFilter

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                .antMatcher("/api/**")
                .addFilterAfter(
                    apiKeyAuthorizationFilter,
                    UsernamePasswordAuthenticationFilter::class.java
                )
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .anyRequest().authenticated()
                .and().csrf().disable()
        }
    }

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE + 2)
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    class WorkerSecurityConfig : WebSecurityConfigurerAdapter() {

        @Autowired
        internal lateinit var properties: ApplicationProperties

        @Autowired
        lateinit var analystAuthenticationFilter: AnalystAuthenticationFilter

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                .antMatcher("/cluster/**")
                .addFilterBefore(analystAuthenticationFilter,
                    UsernamePasswordAuthenticationFilter::class.java)
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .anyRequest().hasAuthority("ANALYST")
        }
    }

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE + 3)
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    class ActuatorSecurityConfig : WebSecurityConfigurerAdapter() {

        @Autowired
        lateinit var apiKeyAuthorizationFilter: ApiKeyAuthorizationFilter

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                .antMatcher("/actuator/**")
                .httpBasic()
                .and()
                .csrf().disable()
                .addFilterBefore(apiKeyAuthorizationFilter,
                    UsernamePasswordAuthenticationFilter::class.java
                )
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .requestMatchers(EndpointRequest.to("metrics", "prometheus"))
                .hasAnyAuthority(Role.SUPERADMIN, Role.PROJADMIN, Perm.MONITOR_SERVER)
                .requestMatchers(EndpointRequest.to("health", "info")).permitAll()
        }
    }

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE + 4)
    @EnableGlobalMethodSecurity(securedEnabled = true, prePostEnabled = true)
    class RootSecurityConfig : WebSecurityConfigurerAdapter() {

        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            http
                .antMatcher("/**")
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/v2/api-docs").permitAll()
                .antMatchers("/error").permitAll()
                .and()
                .csrf().disable()
        }
    }

    @Value("\${management.endpoints.password}")
    lateinit var monitorPassword: String

    @Autowired
    @Throws(Exception::class)
    fun configureGlobal(auth: AuthenticationManagerBuilder) {
        auth
            .authenticationProvider(apiKeyAuthenticationProvider())
            .inMemoryAuthentication()
            .withUser("monitor").password(passwordEncoder().encode(monitorPassword))
            .authorities(Perm.MONITOR_SERVER)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    /**
     * An AuthenticationProvider that handles previously validated JWT claims.
     */
    @Bean
    fun apiKeyAuthenticationProvider(): ApiKeyAuthenticationProvider {
        return ApiKeyAuthenticationProvider()
    }

    @Bean
    fun apiKeyAuthenticationFilter(): ApiKeyAuthorizationFilter {
        return ApiKeyAuthorizationFilter(authServerClient())
    }

    @Bean
    fun analystAuthenticationFilter(): AnalystAuthenticationFilter {
        return AnalystAuthenticationFilter()
    }

    @Bean
    fun authServerClient(): AuthServerClient {
        return AuthServerClientImpl(
            properties.getString("security.auth-server.url"),
            properties.getString("security.service-key"))
    }

    @Bean
    fun apiKeyFilterRegistration(): FilterRegistrationBean<ApiKeyAuthorizationFilter> {
        val bean = FilterRegistrationBean<ApiKeyAuthorizationFilter>()
        bean.filter = apiKeyAuthenticationFilter()
        bean.isEnabled = false
        return bean
    }

    @Bean
    fun analystFilterRegistration(): FilterRegistrationBean<AnalystAuthenticationFilter> {
        val bean = FilterRegistrationBean<AnalystAuthenticationFilter>()
        bean.filter = analystAuthenticationFilter()
        bean.isEnabled = false
        return bean
    }
    companion object {
        private val logger = LoggerFactory.getLogger(MultipleWebSecurityConfig::class.java)
    }
}

package com.zorroa.archivist.security;

import com.zorroa.archivist.config.ArchivistConfiguration;
import com.zorroa.archivist.domain.LogAction;
import com.zorroa.archivist.domain.User;
import com.zorroa.archivist.domain.UserLogSpec;
import com.zorroa.archivist.service.EventLogService;
import com.zorroa.archivist.service.UserService;
import com.zorroa.common.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.ldap.DefaultSpringSecurityContextSource;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;
import org.springframework.security.web.access.channel.ChannelProcessingFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsUtils;

/**
 * Created by chambers on 6/9/16.
 */
@EnableWebSecurity
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MultipleWebSecurityConfig {

    @Autowired
    ApplicationProperties properties;

    @Autowired
    UserDetailsPopulator userDetailsPopulator;

    @Autowired
    UserDetailsPluginWrapper userDetailsPluginWrapper;

    private static final Logger logger = LoggerFactory.getLogger(MultipleWebSecurityConfig.class);

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EnableGlobalMethodSecurity(prePostEnabled=true)
    public static class WebSecurityConfig extends WebSecurityConfigurerAdapter {

        @Autowired
        ApplicationProperties properties;

        @Bean
        public ResetPasswordSecurityFilter resetPasswordSecurityFilter() {
            return new ResetPasswordSecurityFilter();
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .antMatcher("/api/**")
                    .addFilterBefore(new HmacSecurityFilter(
                            properties.getBoolean("archivist.security.hmac.enabled")), UsernamePasswordAuthenticationFilter.class)
                    .addFilterAfter(resetPasswordSecurityFilter(), HmacSecurityFilter.class)
                    .authorizeRequests()
                    .antMatchers("/api/v1/reset-password").permitAll()
                    .antMatchers("/api/v1/send-password-reset-email").permitAll()
                    .antMatchers("/api/v1/send-onboard-email").permitAll()
                    .requestMatchers(CorsUtils::isCorsRequest).permitAll()
                    .anyRequest().authenticated()
                    .and().headers().frameOptions().disable()
                    .and()
                    .httpBasic()
                    .and()
                    .sessionManagement()
                    .and()
                    .csrf().disable();

            if (properties.getBoolean("archivist.debug-mode.enabled")) {
                http.authorizeRequests()
                        .requestMatchers(CorsUtils::isCorsRequest).permitAll()
                .and().addFilterBefore(new CorsCredentialsFilter(), ChannelProcessingFilter.class);
            }
        }
    }

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE+1)
    public static class AdminSecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/admin/**")
                .exceptionHandling()
                .accessDeniedPage("/signin")
                .authenticationEntryPoint(
                        new LoginUrlAuthenticationEntryPoint("/signin"))
                .and()
                .authorizeRequests()
                    .antMatchers("/admin/**").hasAuthority("group::administrator")
                    .and()
                    .sessionManagement()
                    .maximumSessions(5)
                    .expiredUrl("/signin")
                    .and()
                    // Everything below here necessary for console
                    .and().headers().frameOptions().disable()
                    .and()
                    .csrf().disable();
        }
    }

    @Configuration
    @Order(Ordered.HIGHEST_PRECEDENCE+2)
    public static class FormSecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/")
                    .exceptionHandling()
                    .accessDeniedPage("/signin")
                    .authenticationEntryPoint(
                            new LoginUrlAuthenticationEntryPoint("/signin"))
                    .and()
                    .sessionManagement()
                    .maximumSessions(5)
                    .expiredUrl("/signin")
                    .and()
                    .and()
                    .logout().logoutRequestMatcher(
                        new AntPathRequestMatcher("/signout")).logoutSuccessUrl("/signin");
        }
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, UserService userService, EventLogService logService) throws Exception {

        if (properties.getBoolean("archivist.security.ldap.enabled")) {
            auth.authenticationProvider(ldapAuthenticationProvider(userDetailsPopulator, userDetailsPluginWrapper));
        }
        if (properties.getBoolean("archivist.security.hmac.enabled")) {
            auth.authenticationProvider(hmacAuthenticationProvider());
        }
        auth
            .authenticationProvider(authenticationProvider())
            .authenticationEventPublisher(authenticationEventPublisher(userService, logService));

        /**
         * If its a unit test we add our rubber stamp authenticator.
         */
        if (ArchivistConfiguration.Companion.getUnittest()) {
            auth.authenticationProvider(new UnitTestAuthenticationProvider());
        }
    }

    @Bean
    @Autowired
    public AuthenticationEventPublisher authenticationEventPublisher(UserService userService, EventLogService logService) {

        return new AuthenticationEventPublisher() {

            private final Logger logger = LoggerFactory.getLogger(FormSecurityConfig.class);

            @Override
            public void publishAuthenticationSuccess(
                    Authentication authentication) {
                try {
                    User user = (User)authentication.getPrincipal();
                    userService.incrementLoginCounter(user);
                    logService.logAsync(new UserLogSpec()
                            .setAction(LogAction.Login)
                            .setUser(user));
                } catch (Exception e) {
                    // If we throw here, the authentication fails, so if we can't log
                    // it then nobody can login.  Sorry L337 hackers
                    logger.warn("Failed to log user authentication", e);
                    throw new SecurityException(e);
                }
            }

            @Override
            public void publishAuthenticationFailure(
                    AuthenticationException exception,
                    Authentication authentication) {
                logger.info("Failed to authenticate: {}", authentication);
                logService.logAsync(new UserLogSpec()
                        .setAction(LogAction.LoginFailure)
                        .setMessage(authentication.getPrincipal().toString() + " failed to login, reason "
                        + exception.getMessage()));
            }
        };
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        return new ZorroaAuthenticationProvider();
    }

    /**
     * Handles python/java client authentication.
     *
     * @return
     */
    @Bean
    public AuthenticationProvider hmacAuthenticationProvider() {
        return new HmacAuthenticationProvider(properties.getBoolean("archivist.security.hmac.trust"));
    }

    @Bean
    @Autowired
    public AuthenticationProvider ldapAuthenticationProvider(UserDetailsPopulator populator, UserDetailsPluginWrapper userDetailsPluginWrapper) throws Exception {
        String url = properties.getString("archivist.security.ldap.url");
        String base = properties.getString("archivist.security.ldap.base");
        String filter = properties.getString("archivist.security.ldap.filter");

        DefaultSpringSecurityContextSource contextSource = new DefaultSpringSecurityContextSource(url);
        contextSource.setBase(base);
        contextSource.afterPropertiesSet();
        LdapUserSearch ldapUserSearch = new FilterBasedLdapUserSearch("", filter, contextSource);
        BindAuthenticator bindAuthenticator = new BindAuthenticator(contextSource);
        bindAuthenticator.setUserSearch(ldapUserSearch);
        LdapAuthenticationProvider ldapAuthenticationProvider =
                new LdapAuthenticationProvider(bindAuthenticator, userDetailsPluginWrapper);
        ldapAuthenticationProvider.setUserDetailsContextMapper(populator);
        return ldapAuthenticationProvider;
    }
}

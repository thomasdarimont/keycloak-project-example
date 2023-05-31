package com.acme.backend.springboot.users.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

import com.c4_soft.springaddons.security.oauth2.config.synchronised.ExpressionInterceptUrlRegistryPostProcessor;

/**
 * Configuration applied on all web endpoints defined for this application. Any configuration on specific resources is applied in addition to these global
 * rules.
 * <ul>
 * <li>Stateless session (no session kept server-side)</li>
 * <li>CORS set up</li>
 * <li>Require the role "ACCESS" for all api paths</li>
 * <li>JWT converted into Spring token</li>
 * </ul>
 */
@Configuration
@EnableMethodSecurity(securedEnabled = true, proxyTargetClass = true)
public class WebSecurityConfig {

	/**
	 * Default security requires users to be authenticated to all routes but those listed in "permit-all" property (see yaml file)
	 *
	 * @return a custom authorization registry for the routes not listed in "permit-all" property
	 */
	@Bean
	ExpressionInterceptUrlRegistryPostProcessor expressionInterceptUrlRegistryPostProcessor() {
		// @formatter:off
        return (AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry registry) -> registry
        		.requestMatchers("/api/**").access(AccessController::checkAccess)
                .anyRequest().fullyAuthenticated();
        // @formatter:on
	}
}
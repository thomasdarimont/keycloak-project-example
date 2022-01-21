package com.acme.backend.springreactive.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;


/**
 * Enables security annotations via like {@link org.springframework.security.access.prepost.PreAuthorize} and
 * {@link org.springframework.security.access.prepost.PostAuthorize} annotations per-method.
 */
@Configuration
@RequiredArgsConstructor
@EnableReactiveMethodSecurity
class MethodSecurityConfig {

    @Bean
    GrantedAuthoritiesMapper keycloakAuthoritiesMapper() {

        SimpleAuthorityMapper mapper = new SimpleAuthorityMapper();
        mapper.setConvertToUpperCase(true);
        return mapper;
    }

}
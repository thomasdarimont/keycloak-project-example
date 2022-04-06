package com.github.thomasdarimont.apps.bff.config;

import com.github.thomasdarimont.apps.bff.config.keycloak.KeycloakLogoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.HashSet;

@Configuration
@RequiredArgsConstructor
class WebSecurityConfig {

    private final KeycloakLogoutHandler keycloakLogoutHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf().ignoringAntMatchers("/spa/**").csrfTokenRepository(new CookieCsrfTokenRepository());
//        http.sessionManagement(sess -> {
//            sess.sessionAuthenticationStrategy()
//        })

        http.authorizeRequests(arc -> {
            // declarative route configuration
            // add additional routes
            arc.antMatchers("/app/**", "/webjars/**", "/resources/**", "/css/**").permitAll();
            arc.anyRequest().fullyAuthenticated();
        });
        http.oauth2Client();
        http.oauth2Login(o2lc -> {
            //o2lc.userInfoEndpoint().userAuthoritiesMapper(userAuthoritiesMapper());
        });
        http.logout(lc -> {
            lc.addLogoutHandler(keycloakLogoutHandler);
        });

        return http.build();
    }

    private GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            var mappedAuthorities = new HashSet<GrantedAuthority>();

            authorities.forEach(authority -> {
                if (authority instanceof OidcUserAuthority) {
                    var oidcUserAuthority = (OidcUserAuthority) authority;

                    var userInfo = oidcUserAuthority.getUserInfo();

                    // TODO extract roles from userInfo response
//                    List<SimpleGrantedAuthority> groupAuthorities = userInfo.getClaimAsStringList("groups").stream().map(g -> new SimpleGrantedAuthority("ROLE_" + g.toUpperCase())).collect(Collectors.toList());
//                    mappedAuthorities.addAll(groupAuthorities);
                }
            });

            return mappedAuthorities;
        };
    }
}
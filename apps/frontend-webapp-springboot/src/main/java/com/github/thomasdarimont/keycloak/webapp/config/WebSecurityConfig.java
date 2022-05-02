package com.github.thomasdarimont.keycloak.webapp.config;

import com.github.thomasdarimont.keycloak.webapp.support.security.KeycloakLogoutHandler;
import com.github.thomasdarimont.keycloak.webapp.support.security.oauth2.OAuth2AuthorizationRequestCustomizers;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashSet;

@Configuration
@RequiredArgsConstructor
class WebSecurityConfig {

    private final KeycloakLogoutHandler keycloakLogoutHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {

        http.authorizeRequests(arc -> {
            // declarative route configuration
            // add additional routes
            arc.antMatchers("/webjars/**", "/resources/**", "/css/**").permitAll();
            arc.anyRequest().fullyAuthenticated();
        });

        // by default spring security oauth2 client does not support PKCE for confidential clients for auth code grant flow,
        // we explicitly enable the PKCE customization here.
        http.oauth2Client(o2cc -> {
            var oauth2AuthRequestResolver = new DefaultOAuth2AuthorizationRequestResolver( //
                    clientRegistrationRepository, //
                    OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI //
            );
            // Note: backported the OAuth2AuthorizationRequestCustomizers from Spring Security 5.7,
            // replace with original version once Spring Boot support Spring Security 5.7.
            oauth2AuthRequestResolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

            o2cc.authorizationCodeGrant().authorizationRequestResolver(oauth2AuthRequestResolver);
        });

        http.oauth2Login(o2lc -> {
            o2lc.userInfoEndpoint().userAuthoritiesMapper(userAuthoritiesMapper());
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
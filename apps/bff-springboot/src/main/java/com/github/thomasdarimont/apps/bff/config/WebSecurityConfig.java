package com.github.thomasdarimont.apps.bff.config;

import com.github.thomasdarimont.apps.bff.config.keycloak.KeycloakLogoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.HashSet;

@Configuration
@RequiredArgsConstructor
class WebSecurityConfig {

    private final KeycloakLogoutHandler keycloakLogoutHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository, AuthorizationRequestRepository authorizationRequestRepository) throws Exception {

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
        // by default spring security oauth2 client does not support PKCE for confidential clients for auth code grant flow,
        // we explicitly enable the PKCE customization here.
        http.oauth2Client(o2cc -> {
            var oauth2AuthRequestResolver = new DefaultOAuth2AuthorizationRequestResolver( //
                    clientRegistrationRepository, //
                    OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI //
            );
            oauth2AuthRequestResolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
            o2cc.authorizationCodeGrant() //
                    .authorizationRequestResolver(oauth2AuthRequestResolver) //
                    .authorizationRequestRepository(authorizationRequestRepository);
        });
        http.oauth2Login(o2lc -> {
            //o2lc.userInfoEndpoint().userAuthoritiesMapper(userAuthoritiesMapper());
        });
        http.logout(lc -> {
            lc.addLogoutHandler(keycloakLogoutHandler);
        });

        return http.build();
    }

    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
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
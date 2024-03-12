package com.github.thomasdarimont.apps.bff3.config;

import com.github.thomasdarimont.apps.bff3.config.keycloak.KeycloakLogoutHandler;
import com.github.thomasdarimont.apps.bff3.support.HttpSessionOAuth2AuthorizedClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.JdbcOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.util.HashSet;

import static org.springframework.boot.autoconfigure.security.servlet.PathRequest.toH2Console;

@Configuration
@RequiredArgsConstructor
class WebSecurityConfig {

    private final KeycloakLogoutHandler keycloakLogoutHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, //
                                           OAuth2AuthorizedClientService oAuth2AuthorizedClientService, //
                                           ClientRegistrationRepository clientRegistrationRepository, //
                                           AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository //
    ) throws Exception {

        http.csrf(customizer -> {
            customizer.ignoringRequestMatchers("/spa/**") //
                    .ignoringRequestMatchers(toH2Console()) //
                    .csrfTokenRepository(new CookieCsrfTokenRepository());
        });
//        http.sessionManagement(sess -> {
//            sess.sessionAuthenticationStrategy()
//        })

        http.authorizeHttpRequests(arc -> {
            // declarative route configuration
            // add additional routes
            arc.requestMatchers(toH2Console()).permitAll();
            arc.requestMatchers("/app/**", "/webjars/**", "/resources/**", "/css/**").permitAll();
            arc.anyRequest().fullyAuthenticated();
        });

        // for the sake of example disable frame protection
         http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable));

        // by default spring security oauth2 client does not support PKCE for confidential clients for auth code grant flow,
        // we explicitly enable the PKCE customization here.
        http.oauth2Client(o2cc -> {
            var oauth2AuthRequestResolver = new DefaultOAuth2AuthorizationRequestResolver( //
                    clientRegistrationRepository, //
                    OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI //
            );
            oauth2AuthRequestResolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

            o2cc.clientRegistrationRepository(clientRegistrationRepository);
            o2cc.authorizedClientService(oAuth2AuthorizedClientService);
            o2cc.authorizationCodeGrant(acgc -> {
                acgc.authorizationRequestResolver(oauth2AuthRequestResolver) //
                        .authorizationRequestRepository(authorizationRequestRepository);
            });

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

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    public OAuth2AuthorizedClientService oAuth2AuthorizedClientService(JdbcOperations jdbcOps, ClientRegistrationRepository clientRegistrationRepository) {
        //var oauthAuthorizedClientService = new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
//        var oauthAuthorizedClientService = new JdbcOAuth2AuthorizedClientService(jdbcOps, clientRegistrationRepository);
        var oauthAuthorizedClientService = new HttpSessionOAuth2AuthorizedClientService();
        return oauthAuthorizedClientService;
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
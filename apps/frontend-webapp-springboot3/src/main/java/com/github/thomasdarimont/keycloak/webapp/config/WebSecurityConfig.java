package com.github.thomasdarimont.keycloak.webapp.config;

import com.github.thomasdarimont.keycloak.webapp.support.HttpSessionOAuth2AuthorizedClientService;
import com.github.thomasdarimont.keycloak.webapp.support.security.KeycloakLogoutHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.DefaultAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequestEntityConverter;
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
import org.springframework.util.MultiValueMap;

import java.util.HashSet;
import java.util.List;

@Configuration
@RequiredArgsConstructor
class WebSecurityConfig {

    private final KeycloakLogoutHandler keycloakLogoutHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository, CorsEndpointProperties corsEndpointProperties) throws Exception {

        http.authorizeHttpRequests(ahrc -> {
            // declarative route configuration
            // add additional routes
            ahrc.requestMatchers("/webjars/**", "/resources/**", "/css/**", "/auth/register").permitAll();
            ahrc.anyRequest().fullyAuthenticated();
        });

        // by default spring security oauth2 client does not support PKCE for confidential clients for auth code grant flow,
        // we explicitly enable the PKCE customization here.
        http.oauth2Client(o2cc -> {
            var oauth2AuthRequestResolver = new DefaultOAuth2AuthorizationRequestResolver( //
                    clientRegistrationRepository, //
                    OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI //
            );
            oauth2AuthRequestResolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
            o2cc.authorizationCodeGrant(customizer -> {
                customizer.authorizationRequestResolver(oauth2AuthRequestResolver);
            });
        });

        http.oauth2Login(o2lc -> {
            o2lc.userInfoEndpoint(customizer -> {
                customizer.userAuthoritiesMapper(userAuthoritiesMapper());
            });

            // customizeTokenEndpointRequest(o2lc);
        });
        http.logout(lc -> {
            lc.addLogoutHandler(keycloakLogoutHandler);
        });

        return http.build();
    }

    private static void customizeTokenEndpointRequest(OAuth2LoginConfigurer<HttpSecurity> o2lc) {
        // customize the token endpoint request parameters
        o2lc.tokenEndpoint(tec -> {
            DefaultAuthorizationCodeTokenResponseClient accessTokenResponseClient = new DefaultAuthorizationCodeTokenResponseClient();
            accessTokenResponseClient.setRequestEntityConverter(new OAuth2AuthorizationCodeGrantRequestEntityConverter(){
                @Override
                protected MultiValueMap<String, String> createParameters(OAuth2AuthorizationCodeGrantRequest authorizationCodeGrantRequest) {

                    // if used with instance specific backchannel logout url: https://${application.session.host}:4633/webapp/logout
                    MultiValueMap<String, String> parameters = super.createParameters(authorizationCodeGrantRequest);
                    parameters.add("client_session_state", "bubu123");
                    parameters.add("client_session_host", "apps.acme.test");
                    return parameters;
                }
            });
            tec.accessTokenResponseClient(accessTokenResponseClient);
        });
    }

    /**
     * The explicit declaration of {@link AuthorizationRequestRepository} is only necessary, if dynamic user self-registration is required.
     * See {@link com.github.thomasdarimont.keycloak.webapp.web.AuthController#register(HttpServletRequest, HttpServletResponse)}.
     * If this is not needed, this bean can be removed.
     *
     * @return
     */
    @Bean
    public AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository() {
        return new HttpSessionOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    public OAuth2AuthorizedClientService oAuth2AuthorizedClientService(OAuth2AuthorizedClientRepository clientRegistrationRepository) {
//        var oauthAuthorizedClientService = new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepository);
        var oauthAuthorizedClientService = new HttpSessionOAuth2AuthorizedClientService(clientRegistrationRepository);
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
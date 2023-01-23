package com.github.thomasdarimont.keycloak.webapp.support;

import com.github.thomasdarimont.keycloak.webapp.support.keycloakclient.KeycloakClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthorizedClientAccessor {

    private final OAuth2AuthorizedClientService authorizedClientService;

    private final KeycloakClient defaultKeycloakService;

    public OAuth2AuthorizedClient getOAuth2AuthorizedClient(Authentication auth) {

        var authToken = (OAuth2AuthenticationToken) auth;
        var registeredId = authToken.getAuthorizedClientRegistrationId();
        var username = auth.getName();
        var authorizedClient = authorizedClientService.loadAuthorizedClient(registeredId, username);

        if (authorizedClient == null) {
            return null;
        }

        var refreshToken = authorizedClient.getRefreshToken();

        try {
            if (refreshToken == null) {
                return null;
            }

            var introspectResponse = defaultKeycloakService.introspect(refreshToken.getTokenValue());
            var active = introspectResponse.getActive();
            if (active != null && !Boolean.parseBoolean(active)) {
                return null;
            }
        } catch (Exception e) {
            log.warn("Token introspection failed." + e.getMessage());
            return null;
        }

        return authorizedClient;
    }
}

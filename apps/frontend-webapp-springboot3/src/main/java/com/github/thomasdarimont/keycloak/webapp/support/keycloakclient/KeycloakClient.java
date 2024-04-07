package com.github.thomasdarimont.keycloak.webapp.support.keycloakclient;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

public interface KeycloakClient {

    KeycloakIntrospectResponse introspect(String token);

    KeycloakUserInfo userInfo(OAuth2AuthorizedClient authorizedClient, OidcIdToken oidcIdToken);

}

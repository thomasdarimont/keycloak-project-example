package com.github.thomasdarimont.keycloak.webapp.keycloak.client;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;

public interface KeycloakClient {
    /* keycloak standard*/
    String buildActionExecutionRedirect(String action, String redirectAfter);

    default String createUpdatePasswordRedirectUrl(String redirectAfter) {
        return buildActionExecutionRedirect("UPDATE_PASSWORD", redirectAfter);
    }

    default String createAddMfaCredentialRedirectUrl(String redirectAfter) {
        return buildActionExecutionRedirect("CONFIGURE_TOTP", redirectAfter);
    }

    /* oidc standards */
    KeycloakIntrospectionResponse introspect(String tokenValue);

    KeycloakUserInfoResponse userInfoViaProvider(OAuth2AuthorizedClient authorizedClient, OidcIdToken oidcIdToken);

    KeycloakUserInfoResponse userInfoDirect(OAuth2AuthorizedClient authorizedClient);
}

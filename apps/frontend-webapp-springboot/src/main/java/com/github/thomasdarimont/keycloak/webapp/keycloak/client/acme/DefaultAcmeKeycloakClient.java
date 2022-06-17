package com.github.thomasdarimont.keycloak.webapp.keycloak.client.acme;

import com.github.thomasdarimont.keycloak.webapp.keycloak.client.DefaultKeycloakClient;
import com.github.thomasdarimont.keycloak.webapp.support.OAuth2Accessor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

public class DefaultAcmeKeycloakClient extends DefaultKeycloakClient implements AcmeKeycloakClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_STRING_OBJECT_TYPE = ParameterizedTypeReference.forType(Map.class);

    private final AcmeKeycloakClientProperties properties;

    private final OAuth2Accessor oauth2Accessor;

    public DefaultAcmeKeycloakClient(WebClient client, OidcUserService userService, AcmeKeycloakClientProperties properties, OAuth2Accessor oauth2Accessor) {
        super(client, userService, properties);
        this.properties = properties;
        this.oauth2Accessor = oauth2Accessor;
    }

    private OAuth2AuthorizedClient getCurrentOauth2AuthorizedClient() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        var authorizedClient = oauth2Accessor.getAuthorizedClient(auth);
        if (authorizedClient == null) {
            throw new AuthenticationCredentialsNotFoundException("invalid session");
        }
        return authorizedClient;
    }

    @Override
    public Map<String, Object> listSettings() {
        var authorizedClient = getCurrentOauth2AuthorizedClient();
        return this.client.get().uri(properties.getSettingsPath()).attributes(oauth2AuthorizedClient(authorizedClient)).retrieve().bodyToMono(MAP_STRING_OBJECT_TYPE).block(properties.getRequestTimeout());
    }

    @Override
    public void updateSettings(Map<String, Object> settings) {
        var authorizedClient = getCurrentOauth2AuthorizedClient();
        this.client.put().uri(properties.getSettingsPath()).attributes(oauth2AuthorizedClient(authorizedClient)).bodyValue(settings).retrieve().toBodilessEntity().block(properties.getRequestTimeout());
    }

    @Override
    public void triggerDeleteAccount() {
        var authorizedClient = getCurrentOauth2AuthorizedClient();
        this.client.delete().uri(properties.getAccountPath()).attributes(oauth2AuthorizedClient(authorizedClient)).retrieve().toBodilessEntity().block(properties.getRequestTimeout());
    }

    @Override
    public AcmeCredentialInfosResponse listCredentials() {
        var authorizedClient = getCurrentOauth2AuthorizedClient();
        return this.client.get()
                .uri(properties.getCredentialsPath())
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(AcmeCredentialInfosResponse.class)
                .block(properties.getRequestTimeout());
    }

    @Override
    public void deleteCredential(AcmeCredentialDeleteRequest credentialDeleteRequest) {
        var authorizedClient = getCurrentOauth2AuthorizedClient();
        this.client.method(HttpMethod.DELETE).uri(properties.getCredentialsPath()).attributes(oauth2AuthorizedClient(authorizedClient)).bodyValue(credentialDeleteRequest).retrieve().toBodilessEntity().block(properties.getRequestTimeout());
    }

    @Override
    public AcmeApplicationInfoResponse listApplications() {
        var authorizedClient = getCurrentOauth2AuthorizedClient();
        return this.client.get().uri(properties.getApplicationsPath()).attributes(oauth2AuthorizedClient(authorizedClient)).retrieve().bodyToMono(AcmeApplicationInfoResponse.class).block(properties.getRequestTimeout());
    }

}

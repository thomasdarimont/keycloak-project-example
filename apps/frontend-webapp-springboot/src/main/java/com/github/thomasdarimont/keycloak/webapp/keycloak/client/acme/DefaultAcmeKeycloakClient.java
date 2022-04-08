package com.github.thomasdarimont.keycloak.webapp.keycloak.client.acme;

import com.github.thomasdarimont.keycloak.webapp.keycloak.client.DefaultKeycloakClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction.oauth2AuthorizedClient;

@Component
public class DefaultAcmeKeycloakClient extends DefaultKeycloakClient implements AcmeKeycloakClient {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_STRING_OBJECT_TYPE = ParameterizedTypeReference.forType(Map.class);

    private final AcmeKeycloakClientProperties properties;

    public DefaultAcmeKeycloakClient(WebClient client, OidcUserService userService, AcmeKeycloakClientProperties properties) {
        super(client, userService, properties);
        this.properties = properties;
    }

    @Override
    public Map<String, Object> listSettings(OAuth2AuthorizedClient authorizedClient) {
        return this.client.get()
                .uri(properties.getSettingsPath())
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(MAP_STRING_OBJECT_TYPE)
                .block(properties.getRequestTimeout());
    }

    @Override
    public void updateSettings(OAuth2AuthorizedClient authorizedClient, Map<String, Object> settings) {
        this.client.put()
                .uri(properties.getSettingsPath())
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .bodyValue(settings)
                .retrieve()
                .toBodilessEntity()
                .block(properties.getRequestTimeout());
    }

    @Override
    public void triggerDeleteAccount(OAuth2AuthorizedClient authorizedClient) {
        this.client.delete()
                .uri(properties.getAccountPath())
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .toBodilessEntity()
                .block(properties.getRequestTimeout());
    }

    @Override
    public AcmeCredentialInfosResponse listCredentials(OAuth2AuthorizedClient authorizedClient) {
        return this.client.get()
                .uri(properties.getCredentialsPath())
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(AcmeCredentialInfosResponse.class)
                .block(properties.getRequestTimeout());
    }

    @Override
    public void deleteCredential(OAuth2AuthorizedClient authorizedClient, AcmeCredentialDeleteRequest credentialDeleteRequest) {
        this.client.method(HttpMethod.DELETE)
                .uri(properties.getCredentialsPath())
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .bodyValue(credentialDeleteRequest)
                .retrieve()
                .toBodilessEntity()
                .block(properties.getRequestTimeout());
    }

    @Override
    public AcmeApplicationInfoResponse listApplications(OAuth2AuthorizedClient authorizedClient) {
        return this.client.get()
                .uri(properties.getApplicationsPath())
                .attributes(oauth2AuthorizedClient(authorizedClient))
                .retrieve()
                .bodyToMono(AcmeApplicationInfoResponse.class)
                .block(properties.getRequestTimeout());
    }

}

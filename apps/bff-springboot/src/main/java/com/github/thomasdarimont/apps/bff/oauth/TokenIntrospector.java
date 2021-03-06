package com.github.thomasdarimont.apps.bff.oauth;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TokenIntrospector {

    private final OAuth2AuthorizedClientService authorizedClientService;

    private final TokenAccessor tokenAccessor;

    public IntrospectionResult introspectToken(Authentication auth) {

        if (!(auth instanceof OAuth2AuthenticationToken)) {
            return null;
        }

        var authToken = (OAuth2AuthenticationToken) auth;
        var authorizedClient = authorizedClientService.loadAuthorizedClient(
                authToken.getAuthorizedClientRegistrationId(),
                auth.getName()
        );

        if (authorizedClient == null) {
            return null;
        }

        var rt = new RestTemplate();
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var requestBody = new LinkedMultiValueMap<String, String>();
        requestBody.add("client_id", authorizedClient.getClientRegistration().getClientId());
        requestBody.add("client_secret", authorizedClient.getClientRegistration().getClientSecret());
        var accessToken = tokenAccessor.getAccessToken(auth);
        requestBody.add("token", accessToken.getTokenValue());
        requestBody.add("token_type_hint", "access_token");

        var tokenIntrospection = authorizedClient.getClientRegistration().getProviderDetails().getIssuerUri() + "/protocol/openid-connect/token/introspect";
        var responseEntity = rt.postForEntity(tokenIntrospection, new HttpEntity<>(requestBody, headers), IntrospectionResult.class);

        var responseData = responseEntity.getBody();
        if (responseData == null || !responseData.isActive()) {
            return null;
        }

        return responseData;
    }

    @Data
    public static class IntrospectionResult {

        private boolean active;

        private Map<String, Object> data = new HashMap<>();

        @JsonAnySetter
        public void setDataEntry(String key, Object value) {
            data.put(key, value);
        }
    }
}

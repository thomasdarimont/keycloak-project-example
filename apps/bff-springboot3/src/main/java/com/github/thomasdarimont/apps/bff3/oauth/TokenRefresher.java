package com.github.thomasdarimont.apps.bff3.oauth;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * Uses the current Oauth2 refresh token of the current user session to obtain new tokens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenRefresher {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public OAuth2AccessToken refreshTokens(OAuth2AuthorizedClient client) {

        var clientRegistration = client.getClientRegistration();
        var refreshToken = client.getRefreshToken();

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        var requestBody = new LinkedMultiValueMap<String, String>();
        requestBody.add("client_id", clientRegistration.getClientId());
        requestBody.add("client_secret", clientRegistration.getClientSecret());
        requestBody.add("grant_type", "refresh_token");
        requestBody.add("refresh_token", refreshToken.getTokenValue());

        var rt = new RestTemplate();
        var responseEntity = rt.postForEntity(clientRegistration.getProviderDetails().getTokenUri(), new HttpEntity<>(requestBody, headers), AccessTokenResponse.class);
        if (!responseEntity.getStatusCode().is2xxSuccessful()) {
            throw new OAuth2AuthenticationException("token refresh failed");
        }

        var accessTokenResponse = responseEntity.getBody();
        var newAccessTokenValue = accessTokenResponse.access_token;
        var newRefreshTokenValue = accessTokenResponse.refresh_token;

        JWTClaimsSet newAccessTokenClaimsSet;
        JWTClaimsSet newRefreshTokenClaimSet;
        try {
            var newAccessToken = JWTParser.parse(newAccessTokenValue);
            newAccessTokenClaimsSet = newAccessToken.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new OAuth2AuthenticationException("token refresh failed: could not parse access token");
        }

        try {
            var newRefreshToken = JWTParser.parse(newRefreshTokenValue);
            newRefreshTokenClaimSet = newRefreshToken.getJWTClaimsSet();
        } catch (ParseException e) {
            throw new OAuth2AuthenticationException("token refresh failed: could not parse refresh token");
        }

        var accessTokenIat = newAccessTokenClaimsSet.getIssueTime().toInstant();
        var accessTokenExp = newAccessTokenClaimsSet.getExpirationTime().toInstant();
        var newOAuth2AccessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, newAccessTokenValue, accessTokenIat, accessTokenExp);

        var refreshTokenIat = newRefreshTokenClaimSet.getIssueTime().toInstant();
        var refreshTokenExp = newRefreshTokenClaimSet.getExpirationTime().toInstant();
        var newOAuth2RefreshToken = new OAuth2RefreshToken(newRefreshTokenValue, refreshTokenIat, refreshTokenExp);

        var newClient = new OAuth2AuthorizedClient(clientRegistration, client.getPrincipalName(), newOAuth2AccessToken, newOAuth2RefreshToken);
        authorizedClientService.saveAuthorizedClient(newClient, SecurityContextHolder.getContext().getAuthentication());

        return newOAuth2AccessToken;
    }

    @Data
    static class AccessTokenResponse {

        final long createdAtSeconds = System.currentTimeMillis() / 1000;

        String access_token;

        String refresh_token;

        String error;

        int expires_in;

        Map<String, Object> metadata = new HashMap<>();

        @JsonAnySetter
        public void setMetadata(String key, Object value) {
            metadata.put(key, value);
        }
    }
}

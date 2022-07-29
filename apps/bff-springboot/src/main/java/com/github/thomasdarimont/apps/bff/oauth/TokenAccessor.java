package com.github.thomasdarimont.apps.bff.oauth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Provides access to OAuth2 access- and refresh-tokens of an authenticated user.
 */
@Slf4j
@Getter
@Setter
@Component
@RequiredArgsConstructor
public class TokenAccessor {

    private final OAuth2AuthorizedClientService authorizedClientService;

    private final TokenRefresher tokenRefresher;

    private Duration accessTokenExpiresSkew = Duration.ofSeconds(10);

    private boolean tokenRefreshEnabled = true;

    public OAuth2AccessToken getAccessTokenForCurrentUser() {
        return getAccessToken(SecurityContextHolder.getContext().getAuthentication());
    }

    public OAuth2AccessToken getAccessToken(Authentication auth) {

        var client = getOAuth2AuthorizedClient(auth);
        if (client == null) {
            return null;
        }

        var accessToken = client.getAccessToken();
        if (accessToken == null) {
            return null;
        }

        var accessTokenStillValid = isAccessTokenStillValid(accessToken);
        if (!accessTokenStillValid && tokenRefreshEnabled) {
            accessToken = tokenRefresher.refreshTokens(client);
        }

        return accessToken;
    }

    public OAuth2RefreshToken getRefreshToken(Authentication auth) {

        OAuth2AuthorizedClient client = getOAuth2AuthorizedClient(auth);
        if (client == null) {
            return null;
        }
        return client.getRefreshToken();
    }

    private boolean isAccessTokenStillValid(OAuth2AccessToken accessToken) {
        var expiresAt = accessToken.getExpiresAt();
        if (expiresAt == null) {
            return false;
        }
        var exp = expiresAt.minus(accessTokenExpiresSkew == null ? Duration.ofSeconds(0) : accessTokenExpiresSkew);
        var now = Instant.now();

        return now.isBefore(exp);
    }


    private OAuth2AuthorizedClient getOAuth2AuthorizedClient(Authentication auth) {

        var authToken = (OAuth2AuthenticationToken) auth;
        var clientId = authToken.getAuthorizedClientRegistrationId();
        var username = auth.getName();
        return authorizedClientService.loadAuthorizedClient(clientId, username);
    }
}

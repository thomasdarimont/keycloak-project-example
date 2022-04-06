package com.github.thomasdarimont.apps.bff.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TokenAccessor {

    private final OAuth2AuthorizedClientService authorizedClientService;

    public OAuth2AccessToken getAccessTokenForCurrentUser() {
        return getAccessToken(SecurityContextHolder.getContext().getAuthentication());
    }

    public OAuth2AccessToken getAccessToken(Authentication auth) {

        OAuth2AuthorizedClient client = getOAuth2AuthorizedClient(auth);
        if (client == null) {
            return null;
        }
        return client.getAccessToken();
    }

    public OAuth2RefreshToken getRefreshToken(Authentication auth) {

        OAuth2AuthorizedClient client = getOAuth2AuthorizedClient(auth);
        if (client == null) {
            return null;
        }
        return client.getRefreshToken();
    }

    private OAuth2AuthorizedClient getOAuth2AuthorizedClient(Authentication auth) {

        OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) auth;
        String clientId = authToken.getAuthorizedClientRegistrationId();
        String username = auth.getName();
        return authorizedClientService.loadAuthorizedClient(clientId, username);
    }

}

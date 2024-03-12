package com.github.thomasdarimont.apps.bff3.support;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

@RequiredArgsConstructor
public class HttpSessionOAuth2AuthorizedClientService implements OAuth2AuthorizedClientService {

    @Override
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String clientRegistrationId, String principalName) {

        return (T) HttpServletRequestUtils.getCurrentHttpSession(false) //
                .map(sess -> sess.getAttribute(clientRegistrationId)) //
                .orElse(null);
    }

    @Override
    public void saveAuthorizedClient(OAuth2AuthorizedClient authorizedClient, Authentication principal) {

        HttpServletRequestUtils.getCurrentHttpSession(false) //
                .ifPresent(sess -> sess.setAttribute(authorizedClient.getClientRegistration().getRegistrationId(), authorizedClient));
    }

    @Override
    public void removeAuthorizedClient(String clientRegistrationId, String principalName) {

        HttpServletRequestUtils.getCurrentHttpSession(false) //
                .ifPresent(sess -> sess.removeAttribute(clientRegistrationId));
    }
}

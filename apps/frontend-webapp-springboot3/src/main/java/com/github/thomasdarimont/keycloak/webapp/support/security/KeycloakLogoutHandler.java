package com.github.thomasdarimont.keycloak.webapp.support.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;


@Slf4j
@Component
public class KeycloakLogoutHandler implements LogoutHandler {

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication auth) {

        var principal = (DefaultOidcUser) auth.getPrincipal();
        var idToken = principal.getIdToken();

        log.info("Propagate logout to keycloak for user. userId={}", idToken.getSubject());

        var issuerUri = idToken.getIssuer().toString();
        var idTokenValue = idToken.getTokenValue();

        var defaultRedirectUri = generateAppUri(request);

        var logoutUrl = createKeycloakLogoutUrl(issuerUri, idTokenValue, defaultRedirectUri);

        try {
            response.sendRedirect(logoutUrl);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String generateAppUri(HttpServletRequest request) {
        var hostname = request.getServerName() + ":" + request.getServerPort();
        var isStandardHttps = "https".equals(request.getScheme()) && request.getServerPort() == 443;
        var isStandardHttp = "http".equals(request.getScheme()) && request.getServerPort() == 80;
        if (isStandardHttps || isStandardHttp) {
            hostname = request.getServerName();
        }
        return request.getScheme() + "://" + hostname + request.getContextPath() + "/";
    }

    private String createKeycloakLogoutUrl(String issuerUri, String idTokenValue, String defaultRedirectUri) {
        return issuerUri + "/protocol/openid-connect/logout?id_token_hint=" + idTokenValue + "&post_logout_redirect_uri=" + defaultRedirectUri;
    }
}

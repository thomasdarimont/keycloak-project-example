package com.github.thomasdarimont.keycloak.custom.support;

import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.ServerCookie;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.util.CookieHelper;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.UriBuilder;

public class CookieUtils {

    public static String parseCookie(String cookieName, HttpRequest httpRequest) {
        Cookie cookie = httpRequest.getHttpHeaders().getCookies().get(cookieName);
        if (cookie == null) {
            return null;
        }
        return cookie.getValue();
    }

    public static void addCookie(String cookieName, String cookieValue, KeycloakSession session, RealmModel realm, int maxAge) {

        UriBuilder baseUriBuilder = session.getContext().getUri().getBaseUriBuilder();
        // TODO think about narrowing the cookie-path to only contain the /auth path.
        String path = baseUriBuilder.path("realms").path(realm.getName()).path("/").build().getPath();

        ClientConnection connection = session.getContext().getConnection();
        boolean secure = realm.getSslRequired().isRequired(connection);

        ServerCookie.SameSiteAttributeValue sameSiteValue = secure ? ServerCookie.SameSiteAttributeValue.NONE : null;
        CookieHelper.addCookie(cookieName, cookieValue, path, null,// domain
                null, // comment
                maxAge, secure, true, // httponly
                sameSiteValue, // same-site
                session);
    }

}

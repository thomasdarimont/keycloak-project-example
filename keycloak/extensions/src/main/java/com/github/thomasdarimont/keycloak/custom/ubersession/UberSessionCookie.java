package com.github.thomasdarimont.keycloak.custom.ubersession;

import jakarta.ws.rs.core.Cookie;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.util.CookieHelper;

public class UberSessionCookie {

    public static final String UBER_SESSION_COOKIE_NAME = "ubsess";

    public static Cookie readCookie(KeycloakSession session) {
        return CookieHelper.getCookie(session.getContext().getRequestHeaders().getCookies(), UBER_SESSION_COOKIE_NAME);
    }
}

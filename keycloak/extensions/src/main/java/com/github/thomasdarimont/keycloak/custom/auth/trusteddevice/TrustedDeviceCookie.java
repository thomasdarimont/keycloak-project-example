package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice;

import com.github.thomasdarimont.keycloak.custom.support.CookieUtils;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Optional;

public class TrustedDeviceCookie {

    public static final String COOKIE_NAME = Optional.ofNullable(System.getenv("KEYCLOAK_AUTH_TRUSTED_DEVICE_COOKIE_NAME")).orElse("ACME_KEYCLOAK_DEVICE");

    public static void removeDeviceCookie(KeycloakSession session, RealmModel realm) {
        // maxAge = 1 triggers legacy cookie removal
        CookieUtils.addCookie(COOKIE_NAME, "", session, realm, 1);
    }

    public static void addDeviceCookie(String deviceTokenString, int maxAge, KeycloakSession session, RealmModel realm) {
        CookieUtils.addCookie(COOKIE_NAME, deviceTokenString, session, realm, maxAge);
    }

    public static TrustedDeviceToken parseDeviceTokenFromCookie(HttpRequest httpRequest, KeycloakSession session) {
        String cookieValue = CookieUtils.parseCookie(COOKIE_NAME, httpRequest);

        if(cookieValue == null) {
            return null;
        }

        // decodes and validates device cookie
        return session.tokens().decode(cookieValue, TrustedDeviceToken.class);
    }

}
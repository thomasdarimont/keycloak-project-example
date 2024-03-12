package com.github.thomasdarimont.keycloak.custom.support;

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.NewCookie;
import org.keycloak.http.HttpResponse;
import org.keycloak.models.KeycloakSession;

import java.util.Map;

public class CookieHelper {

    public static final String LEGACY_COOKIE = "_LEGACY";

    /**
     * Set a response cookie.  This solely exists because JAX-RS 1.1 does not support setting HttpOnly cookies
     * @param name
     * @param value
     * @param path
     * @param domain
     * @param comment
     * @param maxAge
     * @param secure
     * @param httpOnly
     * @param sameSite
     */
    public static void addCookie(String name, String value, String path, String domain, String comment, int maxAge, boolean secure, boolean httpOnly, NewCookie.SameSite sameSite, KeycloakSession session) {
        NewCookie.SameSite sameSiteParam = sameSite;
        // when expiring a cookie we shouldn't set the sameSite attribute; if we set e.g. SameSite=None when expiring a cookie, the new cookie (with maxAge == 0)
        // might be rejected by the browser in some cases resulting in leaving the original cookie untouched; that can even prevent user from accessing their application
        if (maxAge == 0) {
            sameSite = null;
        }

        boolean secure_sameSite = sameSite == NewCookie.SameSite.NONE || secure; // when SameSite=None, Secure attribute must be set

        HttpResponse response = session.getContext().getHttpResponse();
        NewCookie cookie = new NewCookie.Builder(name) //
                .value(value) //
                .path(path) //
                .domain(domain) //
                .comment(comment) //
                .maxAge(maxAge) //
                .secure(secure_sameSite) //
                .sameSite(sameSite) //
                .httpOnly(httpOnly)
                .build();

        response.setCookieIfAbsent(cookie);

        // a workaround for browser in older Apple OSs – browsers ignore cookies with SameSite=None
        if (sameSiteParam == NewCookie.SameSite.NONE) {
            addCookie(name + LEGACY_COOKIE, value, path, domain, comment, maxAge, secure, httpOnly, null, session);
        }
    }

    /**
     * Set a response cookie avoiding SameSite parameter
     * @param name
     * @param value
     * @param path
     * @param domain
     * @param comment
     * @param maxAge
     * @param secure
     * @param httpOnly
     */
    public static void addCookie(String name, String value, String path, String domain, String comment, int maxAge, boolean secure, boolean httpOnly, KeycloakSession session) {
        addCookie(name, value, path, domain, comment, maxAge, secure, httpOnly, null, session);
    }

    public static String getCookieValue(KeycloakSession session, String name) {
        Map<String, Cookie> cookies = session.getContext().getRequestHeaders().getCookies();
        Cookie cookie = cookies.get(name);
        if (cookie == null) {
            String legacy = name + LEGACY_COOKIE;
            cookie = cookies.get(legacy);
        }
        return cookie != null ? cookie.getValue() : null;
    }

}
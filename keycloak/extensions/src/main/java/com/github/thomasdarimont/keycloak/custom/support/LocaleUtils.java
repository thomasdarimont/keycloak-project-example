package com.github.thomasdarimont.keycloak.custom.support;

import org.keycloak.http.HttpRequest;
import org.keycloak.models.RealmModel;

import java.util.Locale;

public class LocaleUtils {

    public static Locale extractLocaleWithFallbackToRealmLocale(HttpRequest request, RealmModel realm) {

        if (request == null && realm == null) {
            return Locale.getDefault();
        }

        if (request == null) {
            return new Locale(realm.getDefaultLocale());
        }

        String kcLocale = request.getUri().getQueryParameters().getFirst("kc_locale");
        if (kcLocale != null ){
            return new Locale(kcLocale);
        }

        return request.getHttpHeaders().getAcceptableLanguages().stream().findFirst().orElseGet(() -> new Locale(realm.getDefaultLocale()));
    }
}

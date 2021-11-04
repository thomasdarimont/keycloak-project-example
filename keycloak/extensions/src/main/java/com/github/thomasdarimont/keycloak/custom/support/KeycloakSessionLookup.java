package com.github.thomasdarimont.keycloak.custom.support;

import org.keycloak.common.util.Resteasy;
import org.keycloak.models.KeycloakSession;

public class KeycloakSessionLookup {

    /**
     * Exposes the current {@link KeycloakSession} from the {@link Resteasy} provider.
     * <p>
     * Note: This can only be called during Resteasy HTTP request processing.
     *
     * @return the session or {@literal null}
     */
    public static KeycloakSession currentSession() {
        return Resteasy.getContextData(KeycloakSession.class);
    }
}

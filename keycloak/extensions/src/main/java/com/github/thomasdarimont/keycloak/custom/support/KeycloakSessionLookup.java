package com.github.thomasdarimont.keycloak.custom.support;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSession;

public class KeycloakSessionLookup {

    /**
     * Exposes the current {@link KeycloakSession} from the {@link ResteasyProviderFactory}.
     * <p>
     * Note: This can only be called during Resteasy HTTP request processing.
     *
     * @return the session or {@literal null}
     */
    public static KeycloakSession currentSession() {
        return ResteasyProviderFactory.getContextData(KeycloakSession.class);
    }
}

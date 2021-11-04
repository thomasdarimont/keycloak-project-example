package com.github.thomasdarimont.keycloak.custom.support;

import org.keycloak.common.util.Resteasy;

public class KeycloakUtil {

    public static boolean isRunningOnKeycloak() {
        return Holder.RUNNING_ON_KEYCLOAK;
    }

    public static boolean isRunningOnKeycloakX() {
        return !isRunningOnKeycloak();
    }

    private static class Holder {

        private static final boolean RUNNING_ON_KEYCLOAK;

        static {
            RUNNING_ON_KEYCLOAK = Resteasy.getProvider().getClass().getSimpleName().equals("Resteasy3Provider");
        }
    }
}

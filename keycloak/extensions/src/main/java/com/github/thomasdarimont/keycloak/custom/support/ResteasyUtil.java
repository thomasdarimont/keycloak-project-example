package com.github.thomasdarimont.keycloak.custom.support;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class ResteasyUtil {

    public static void injectProperties(Object instance) {
        ResteasyProviderFactory.getInstance().injectProperties(instance);
    }
}

package com.github.thomasdarimont.keycloak.custom.endpoints;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

@JBossLog
@AutoService(RealmResourceProviderFactory.class)
public class CustomResourceProviderFactory implements RealmResourceProviderFactory {

    private static final CustomResourceProvider INSTANCE = new CustomResourceProvider();

    @Override
    public String getId() {
        return CustomResourceProvider.ID;
    }

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return INSTANCE;
    }

    @Override
    public void init(Config.Scope config) {
        // NOOP

    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        log.info("Initialize");
    }

    @Override
    public void close() {
        // NOOP
    }
}

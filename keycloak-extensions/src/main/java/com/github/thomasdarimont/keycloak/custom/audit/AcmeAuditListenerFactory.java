package com.github.thomasdarimont.keycloak.custom.audit;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

@AutoService(EventListenerProviderFactory.class)
public class AcmeAuditListenerFactory implements EventListenerProviderFactory {

    private static final AcmeAuditListener INSTANCE = new AcmeAuditListener();

    @Override
    public String getId() {
        return AcmeAuditListener.ID;
    }

    @Override // return singleton instance, create new AcmeAuditListener(session) or use lazy initialization
    public EventListenerProvider create(KeycloakSession session) {
        return INSTANCE;
    }

    @Override // we could read settings from the provider config in standalone(-ha).xml
    public void init(Config.Scope config) {
        /* configure factory */
    }

    @Override // we could init our provider with information from other providers
    public void postInit(KeycloakSessionFactory factory) { /* post-process factory */ }

    @Override // close resources if necessary
    public void close() { /* release resources if necessary */ }
}

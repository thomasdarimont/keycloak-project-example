package com.github.thomasdarimont.keycloak.custom.eventpublishing;

import com.google.auto.service.AutoService;
import lombok.RequiredArgsConstructor;
import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

@RequiredArgsConstructor
public class AcmeEventPublisherEventListener implements EventListenerProvider {

    public static final String ID = "acme-event-publisher";

    private final KeycloakSession session;

    @Override
    public void onEvent(Event event) {
        // NOOP
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {

    }

    @Override
    public void close() {

    }

    @AutoService(EventListenerProviderFactory.class)
    public static class Factory implements EventListenerProviderFactory {

        @Override
        public String getId() {
            return ID;
        }

        @Override // return singleton instance, create new AcmeAuditListener(session) or use lazy initialization
        public EventListenerProvider create(KeycloakSession session) {
            return new AcmeEventPublisherEventListener(session);
        }

        @Override
        public void init(Config.Scope config) {
            /* configure factory */
        }

        @Override // we could init our provider with information from other providers
        public void postInit(KeycloakSessionFactory factory) { /* post-process factory */ }

        @Override // close resources if necessary
        public void close() { /* release resources if necessary */ }
    }
}

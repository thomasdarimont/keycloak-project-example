package com.github.thomasdarimont.keycloak.custom.eventpublishing;

import com.google.auto.service.AutoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

import java.util.Map;

@JBossLog
@RequiredArgsConstructor
public class AcmeEventPublisherEventListener implements EventListenerProvider {

    public static final String ID = "acme-event-publisher";

    private final KeycloakSession session;

    private final EventPublisher publisher;

    @Override
    public void onEvent(Event event) {
        publisher.publish("acme.iam.keycloak.user", enrichUserEvent(event));
    }

    private Object enrichUserEvent(Event event) {
        return event;
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        publisher.publish("acme.iam.keycloak.admin", enrichAdminEvent(event, includeRepresentation));
    }

    private Object enrichAdminEvent(AdminEvent event, boolean includeRepresentation) {
        return event;
    }

    @Override
    public void close() {
        // NOOP
    }

    @AutoService(EventListenerProviderFactory.class)
    public static class Factory implements EventListenerProviderFactory, ServerInfoAwareProviderFactory {

        private EventPublisher publisher;

        @Override
        public String getId() {
            return ID;
        }

        @Override // return singleton instance, create new AcmeAuditListener(session) or use lazy initialization
        public EventListenerProvider create(KeycloakSession session) {
            return new AcmeEventPublisherEventListener(session, publisher);
        }

        @Override
        public void init(Config.Scope config) {
            /* configure factory */
            try {
                publisher = createNatsPublisher(config);
            } catch (Exception ex) {
                log.warnf("Could not create nats publisher: %s", ex.getMessage());
                publisher = new NoopPublisher();
            }
        }

        private NatsEventPublisher createNatsPublisher(Config.Scope config) {

            String url = config.get("nats-url", "nats://acme-nats:4222");
            String username = config.get("nats-username", "keycloak");
            String password = config.get("nats-password", "keycloak");

            var nats = new NatsEventPublisher(url, username, password);
            nats.init();

            log.info("Created new NatsPublisher");

            return nats;
        }

        @Override // we could init our provider with information from other providers
        public void postInit(KeycloakSessionFactory factory) { /* post-process factory */ }

        @Override // close resources if necessary
        public void close() {
            if (publisher != null) {
                publisher.close();
            }
        }

        @Override
        public Map<String, String> getOperationalInfo() {
            return publisher.getOperationalInfo();
        }
    }
}

package demo.events;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

@AutoService(EventListenerProviderFactory.class)
public class MyEventListener implements EventListenerProvider, EventListenerProviderFactory {

    @Override
    public String getId() {
        return "myevents";
    }

    @Override
    public void onEvent(Event event) {
        System.out.println("UserEvent: " + event);
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        System.out.println("AdminEvent: " + event);
    }

    @Override
    public EventListenerProvider create(KeycloakSession session) {
        return new MyEventListener();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

}

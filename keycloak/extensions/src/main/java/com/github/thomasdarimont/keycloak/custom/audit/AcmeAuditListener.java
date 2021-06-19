package com.github.thomasdarimont.keycloak.custom.audit;

import lombok.extern.jbosslog.JBossLog;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;


@JBossLog
public class AcmeAuditListener implements EventListenerProvider {

    public static final String ID = "acme-audit-listener";

    @Override
    public void onEvent(Event event) {
        // called for each UserEvent’s
        log.infof("Forward to audit service: audit userEvent %s", event.getType());
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRep) {
        // called for each AdminEvent’s
        // log.infof("Forward to audit service: audit adminEvent %s", event);
    }

    @Override
    public void close() {
        // called after component use
    }
}

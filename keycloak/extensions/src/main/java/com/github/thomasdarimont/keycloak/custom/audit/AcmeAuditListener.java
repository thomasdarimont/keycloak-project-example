package com.github.thomasdarimont.keycloak.custom.audit;

import lombok.extern.jbosslog.JBossLog;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerTransaction;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;


@JBossLog
public class AcmeAuditListener implements EventListenerProvider {

    public static final String ID = "acme-audit-listener";

    private final KeycloakSession session;

    private final EventListenerTransaction tx;

    public AcmeAuditListener(KeycloakSession session) {
        this.session = session;
        this.tx = new EventListenerTransaction(this::processAdminEventAfterTransaction, this::processUserEventAfterTransaction);
        session.getTransactionManager().enlistAfterCompletion(tx);
    }

    @Override
    public void onEvent(Event event) {
        tx.addEvent(event);
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRep) {
        tx.addAdminEvent(event, includeRep);
    }

    private void processUserEventAfterTransaction(Event event) {
        // called for each UserEvent’s
        log.infof("Forward to audit service: audit userEvent %s", event.getType());
    }

    private void processAdminEventAfterTransaction(AdminEvent event, boolean includeRep) {
        // called for each AdminEvent’s
        // log.infof("Forward to audit service: audit adminEvent %s", event);
    }

    @Override
    public void close() {
        // called after component use
    }
}

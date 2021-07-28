package com.github.thomasdarimont.keycloak.custom.profile.emailupdate;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.authentication.DisplayTypeRequiredActionFactory;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

@AutoService(RequiredActionFactory.class)
public class UpdateEmailRequiredActionFactory implements RequiredActionFactory, DisplayTypeRequiredActionFactory {

    private static final UpdateEmailRequiredAction INSTANCE = new UpdateEmailRequiredAction();

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return INSTANCE;
    }

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        return create(session);
    }

    @Override
    public void init(Config.Scope config) {
        // NOOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

    @Override
    public String getId() {
        return UpdateEmailRequiredAction.ID;
    }

    @Override
    public String getDisplayText() {
        return "Acme: Update Email";
    }

    @Override
    public boolean isOneTimeAction() {
        return true;
    }
}

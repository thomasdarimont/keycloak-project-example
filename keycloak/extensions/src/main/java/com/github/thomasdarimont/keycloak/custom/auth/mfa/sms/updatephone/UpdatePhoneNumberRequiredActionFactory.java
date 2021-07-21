package com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.updatephone;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.authentication.DisplayTypeRequiredActionFactory;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

@AutoService(RequiredActionFactory.class)
public class UpdatePhoneNumberRequiredActionFactory implements RequiredActionFactory, DisplayTypeRequiredActionFactory {

    private static final RequiredActionProvider INSTANCE = new UpdatePhoneNumberRequiredAction();

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
        return UpdatePhoneNumberRequiredAction.ID;
    }

    @Override
    public String getDisplayText() {
        return "Acme: Update Mobile Phonenumber";
    }

    @Override
    public boolean isOneTimeAction() {
        return true;
    }
}

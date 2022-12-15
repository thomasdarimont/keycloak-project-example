package com.github.thomasdarimont.keycloak.custom.account;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

@JBossLog
public class AccountPostLoginAction implements RequiredActionProvider {

    public static final String LAST_SUCCESS_LOGIN_ATTR = "lastSuccessfulLoginTimestamp";

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

        var authSession = context.getAuthenticationSession();
        if (authSession.getAuthNote(getClass().getSimpleName()) != null) {
            return; // action was already executed
        }
        authSession.setAuthNote(getClass().getSimpleName(), "true");

        log.infof("Post-processing account");

        var user = context.getUser();
        user.setSingleAttribute(LAST_SUCCESS_LOGIN_ATTR, String.valueOf(System.currentTimeMillis()));
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        // NOOP
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

    @AutoService(RequiredActionFactory.class)
    public static class Factory implements RequiredActionFactory {

        private static final AccountPostLoginAction INSTANCE = new AccountPostLoginAction();

        @Override
        public String getId() {
            return "acme-account-post-processing";
        }

        @Override
        public String getDisplayText() {
            return "Acme Account Post-Processing";
        }

        @Override
        public RequiredActionProvider create(KeycloakSession session) {
            return INSTANCE;
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

    }
}

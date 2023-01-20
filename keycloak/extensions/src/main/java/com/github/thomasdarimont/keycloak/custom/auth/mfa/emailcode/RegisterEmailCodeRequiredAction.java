package com.github.thomasdarimont.keycloak.custom.auth.mfa.emailcode;

import com.github.thomasdarimont.keycloak.custom.account.AccountActivity;
import com.github.thomasdarimont.keycloak.custom.account.MfaChange;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

@JBossLog
public class RegisterEmailCodeRequiredAction implements RequiredActionProvider {

    public static final String ID = "acme-register-email-code";

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        // NOOP
    }

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        // we want to trigger that action via kc_actions URL parameter in the auth url
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {

        var session = context.getSession();
        var user = context.getUser();
        var realm = context.getRealm();
        var credentialManager = user.credentialManager();
        credentialManager.getStoredCredentialsByTypeStream(EmailCodeCredentialModel.TYPE).forEach(cm -> credentialManager.removeStoredCredentialById(cm.getId()));

        var model = new EmailCodeCredentialModel();
        model.setCreatedDate(Time.currentTimeMillis());

        var credential = user.credentialManager().createStoredCredential(model);
        if (credential != null) {
            AccountActivity.onUserMfaChanged(session, realm, user, credential, MfaChange.ADD);
        }

        context.success();
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

        private static final RequiredActionProvider INSTANCE = new RegisterEmailCodeRequiredAction();

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

        @Override
        public String getId() {
            return RegisterEmailCodeRequiredAction.ID;
        }

        @Override
        public String getDisplayText() {
            return "Acme: Register MFA via E-Mail code";
        }

        @Override
        public boolean isOneTimeAction() {
            return true;
        }
    }
}

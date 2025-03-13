package com.github.thomasdarimont.keycloak.custom.auth.demo;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.Response;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.List;
import java.util.Optional;

public class SkippableRequiredAction implements RequiredActionProvider {

    public static final String PROVIDER_ID = "ACME_DEMO_SKIPPABLE_ACTION";

    public static final String ACTION_SKIPPED_SESSION_NOTE = PROVIDER_ID + ":skipped";

    public static final String SKIP_COUNT_USER_ATTRIBUTE = "acme-action-count";

    public static final String ACTION_DONE_USER_ATTRIBUTE = "acme-action-done";

    public static final String MAX_SKIP_COUNT_CONFIG_ATTRIBUTE = "max-skip-count";

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        // check if evaluate triggers was already called for this required action
        if (authSession.getAuthNote(PROVIDER_ID) != null) {
            return;
        }
        authSession.setAuthNote(PROVIDER_ID, "");

        UserModel user = context.getUser();
        if (!isUserActionRequired(user)) {
            return;
        }

        if (didUserSkipRequiredAction(context, authSession)) {
            return;
        }

        authSession.addRequiredAction(PROVIDER_ID);
    }

    protected boolean didUserSkipRequiredAction(RequiredActionContext context, AuthenticationSessionModel authSession) {
        // we remember the action skipping in the user session to have it available for every auth interaction within the current user session
        UserSessionModel userSession = context.getSession().sessions().getUserSession(context.getRealm(), authSession.getParentSession().getId());
        return userSession != null && "true".equals(userSession.getNote(ACTION_SKIPPED_SESSION_NOTE));
    }

    protected boolean isUserActionRequired(UserModel user) {
        return user.getFirstAttribute(ACTION_DONE_USER_ATTRIBUTE) == null;
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {

        Response challenge = createChallengeForm(context);
        context.challenge(challenge);
    }

    protected Response createChallengeForm(RequiredActionContext context) {
        LoginFormsProvider form = context.form();

        boolean canSkip = isSkipActionPossible(context);
        form.setAttribute("canSkip", canSkip);

        return form.createForm("login-skippable-action.ftl");
    }

    protected boolean isSkipActionPossible(RequiredActionContext context) {

        UserModel user = context.getUser();

        int skipCount = Integer.parseInt(Optional.ofNullable(user.getFirstAttribute(SKIP_COUNT_USER_ATTRIBUTE)).orElse("0"));
        String maxSkipCountConfigValue = context.getConfig().getConfigValue(MAX_SKIP_COUNT_CONFIG_ATTRIBUTE);

        if (maxSkipCountConfigValue == null) {
            return false;
        }

        int maxSkipCount = Integer.parseInt(maxSkipCountConfigValue);
        return skipCount < maxSkipCount;
    }

    @Override
    public void processAction(RequiredActionContext context) {

        var formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.containsKey("skip")) {

            if (!isSkipActionPossible(context)) {
                // nice try sneaky hacker
                Response challenge = createChallengeForm(context);
                context.challenge(challenge);
                return;
            }

            recordActionSkipped(context.getUser(), context.getAuthenticationSession());

            context.success();
            return;
        }

        markActionDone(context.getUser());

        context.success();
    }

    protected void markActionDone(UserModel user) {
        user.setSingleAttribute(ACTION_DONE_USER_ATTRIBUTE, Boolean.toString(true));
        user.removeAttribute(SKIP_COUNT_USER_ATTRIBUTE);
    }

    protected void recordActionSkipped(UserModel user, AuthenticationSessionModel authSession) {
        int skipCount = Integer.parseInt(Optional.ofNullable(user.getFirstAttribute(SKIP_COUNT_USER_ATTRIBUTE)).orElse("0"));
        skipCount+=1;
        user.setSingleAttribute(SKIP_COUNT_USER_ATTRIBUTE, Integer.toString(skipCount));

        authSession.setUserSessionNote(ACTION_SKIPPED_SESSION_NOTE, "true");
    }

    @Override
    public void close() {
        // NOOP
    }

    @AutoService(RequiredActionFactory.class)
    public static class Factory implements RequiredActionFactory {

        private static final SkippableRequiredAction INSTANCE = new SkippableRequiredAction();

        @Override
        public String getId() {
            return PROVIDER_ID;
        }

        @Override
        public String getDisplayText() {
            return "Acme: Skippable Action";
        }

        @Override
        public RequiredActionProvider create(KeycloakSession session) {
            return INSTANCE;
        }

        @Override
        public List<ProviderConfigProperty> getConfigMetadata() {

            List<ProviderConfigProperty> configProperties = ProviderConfigurationBuilder.create() //
                    .property() //
                    .name(MAX_SKIP_COUNT_CONFIG_ATTRIBUTE) //
                    .label("Max Skip") //
                    .required(true) //
                    .defaultValue(2) //
                    .helpText("Maximum skip count") //
                    .type(ProviderConfigProperty.INTEGER_TYPE) //
                    .add() //
                    .build();
            return configProperties;
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

package com.github.thomasdarimont.keycloak.custom.auth.magiclink;

import com.github.thomasdarimont.keycloak.custom.support.RealmUtils;
import com.github.thomasdarimont.keycloak.custom.support.UserUtils;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MagicLinkAuthenticator implements Authenticator {

    public static final String ID = "acme-magic-link";

    private static final String MAGIC_LINK_KEY = "magic-link-key";
    private static final String QUERY_PARAM = "acme_magic_link_key";

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        var sessionKey = context.getAuthenticationSession().getAuthNote(MAGIC_LINK_KEY);

        if (sessionKey == null) {
            var user = context.getUser();
            if (user == null) {
                // to avoid account enumeration, we show the success page anyways.
                displayMagicLinkPage(context);
                return;
            }

            sendMagicLink(context);
            return;
        }

        var requestKey = context.getHttpRequest().getUri().getQueryParameters().getFirst(QUERY_PARAM);
        if (requestKey == null) {
            displayMagicLinkPage(context);
            return;
        }

        context.getEvent().detail("authenticator", ID);
        if (requestKey.equals(sessionKey)) {
            context.success();
        } else {
            context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
            context.getEvent().detail("error", "magicSessionKey mismatch");
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        // NOOP
    }

    private void sendMagicLink(AuthenticationFlowContext context) {

        var magicLinkSessionKey = KeycloakModelUtils.generateId();
        context.getAuthenticationSession().setAuthNote(MAGIC_LINK_KEY, magicLinkSessionKey);

        var emailTemplateProvider = context.getSession().getProvider(EmailTemplateProvider.class);
        emailTemplateProvider.setRealm(context.getRealm());
        emailTemplateProvider.setUser(context.getUser());

        var magicLink = generateMagicLink(context, magicLinkSessionKey);
        // for further processing we need a mutable map here
        Map<String, Object> msgParams = new HashMap<>();
        msgParams.put("userDisplayName", UserUtils.deriveDisplayName(context.getUser()));
        msgParams.put("link", magicLink);

        var subjectParams = List.<Object>of(RealmUtils.getDisplayName(context.getRealm()));

        try {
            emailTemplateProvider.send("acmeMagicLinkEmailSubject", subjectParams, "acme-magic-link.ftl", msgParams);
            displayMagicLinkPage(context);
        } catch (EmailException e) {
            log.error("Could not send magic link per email.", e);
            context.failure(AuthenticationFlowError.INTERNAL_ERROR);
        }
    }

    private String generateMagicLink(AuthenticationFlowContext context, String magicLinkSessionKey) {
        // TODO generate an Application initiated Action link to allow opening the link on with other devices.
        return KeycloakUriBuilder.fromUri(context.getRefreshExecutionUrl()).queryParam(QUERY_PARAM, magicLinkSessionKey).build().toString();
    }

    private void displayMagicLinkPage(AuthenticationFlowContext context) {
        var form = context.form().setAttribute("skipLink", true);
        form.setInfo("acmeMagicLinkText");
        context.challenge(form.createForm("login-magic-link.ftl"));
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

    @AutoService(AuthenticatorFactory.class)
    public static class Factory implements AuthenticatorFactory {

        private static final MagicLinkAuthenticator INSTANCE = new MagicLinkAuthenticator();

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public Authenticator create(KeycloakSession session) {
            return INSTANCE;
        }

        @Override
        public String getDisplayType() {
            return "Acme: MagicLink";
        }

        @Override
        public String getHelpText() {
            return "Allows the user to login with a link sent via email.";
        }

        @Override
        public String getReferenceCategory() {
            return "passwordless";
        }

        @Override
        public boolean isConfigurable() {
            return false;
        }

        @Override
        public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
            return REQUIREMENT_CHOICES;
        }

        @Override
        public boolean isUserSetupAllowed() {
            return false;
        }


        @Override
        public List<ProviderConfigProperty> getConfigProperties() {
            return null;
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

}
package com.github.thomasdarimont.keycloak.custom.registration.formaction;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.RealmBean;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This welcome action can be placed as the last step of a custom registration flow to send an welcome-email to the new user.
 */
@JBossLog
public class WelcomeEmailFormAction implements FormAction {

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {
        // NOOP
    }

    @Override
    public void validate(ValidationContext context) {
        context.success();
    }

    @Override
    public void success(FormContext context) {

        var session = context.getSession();
        var realm = context.getRealm();
        var user = context.getUser();

        var username = user.getUsername();
        var userDisplayName = getUserDisplayName(user);

        // NOOP
        Map<String, Object> mailBodyAttributes = new HashMap<>();
        mailBodyAttributes.put("realm", new RealmBean(realm));
        mailBodyAttributes.put("username", username);
        mailBodyAttributes.put("userDisplayName", userDisplayName);

        var realmDisplayName = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();

        List<Object> subjectParams = List.of(realmDisplayName, userDisplayName);

        try {
            var emailProvider = session.getProvider(EmailTemplateProvider.class);
            emailProvider.setRealm(realm);
            emailProvider.setUser(user);
            // Don't forget to add the acme-welcome.ftl (html and text) template to your theme.
            emailProvider.send("acmeWelcomeSubject", subjectParams, "acme-welcome.ftl", mailBodyAttributes);
        } catch (EmailException eex) {
            log.errorf(eex, "Failed to send welcome email. realm=%s user=%s", realm.getName(), username);
        }
    }

    private String getUserDisplayName(UserModel user) {

        var firstname = user.getFirstName();
        var lastname = user.getLastName();

        if (firstname != null && lastname != null) {
            return firstname + " " + lastname;
        }

        return user.getUsername();
    }

    @Override
    public boolean requiresUser() {
        // must return false here during registration.
        return false;
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

    @AutoService(FormActionFactory.class)
    public static class Factory implements FormActionFactory {

        private static final WelcomeEmailFormAction INSTANCE = new WelcomeEmailFormAction();

        public static final String ID = "acme-welcome-form-action";

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public String getDisplayType() {
            return "Acme: Welcome mail";
        }

        @Override
        public String getHelpText() {
            return "Sends a welcome mail to a newly registered user.";
        }

        @Override
        public String getReferenceCategory() {
            return "Post Processing";
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
            return Collections.emptyList();
        }

        @Override
        public FormAction create(KeycloakSession session) {
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

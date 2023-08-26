package com.github.thomasdarimont.keycloak.custom.security.friendlycaptcha;

import com.github.thomasdarimont.keycloak.custom.support.LocaleUtils;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.FormAction;
import org.keycloak.authentication.FormActionFactory;
import org.keycloak.authentication.FormContext;
import org.keycloak.authentication.ValidationContext;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Collections;
import java.util.List;

@JBossLog
public class FriendlyCaptchaFormAction implements FormAction {

    @Override
    public void buildPage(FormContext context, LoginFormsProvider form) {

        var locale = LocaleUtils.extractLocaleWithFallbackToRealmLocale(context.getHttpRequest(), context.getRealm());
        var captcha = new FriendlyCaptcha(context.getSession());
        captcha.configureForm(form, locale);
    }

    @Override
    public void validate(ValidationContext context) {

        var captcha = new FriendlyCaptcha(context.getSession());

        var formData = context.getHttpRequest().getDecodedFormParameters();
        var solutionFieldName = captcha.getConfig().getSolutionFieldName();

        var verificationResult = captcha.verifySolution(formData);
        if (!verificationResult.isSuccessful()) {
            String errorMessage = verificationResult.getErrorMessage();
            context.error(Errors.INVALID_REGISTRATION);
            formData.remove(solutionFieldName);
            context.validationError(formData, List.of(new FormMessage(solutionFieldName, errorMessage)));
            return;
        }

        context.success();
    }

    @Override
    public void success(FormContext context) {
        log.debug("Friendly captcha verification successful");
    }

    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return false;
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

        private static final FriendlyCaptchaFormAction INSTANCE = new FriendlyCaptchaFormAction();

        public static final String ID = "acme-friendly-captcha-form-action";

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public String getDisplayType() {
            return "Acme: Friendly Captcha";
        }

        @Override
        public String getHelpText() {
            return "Generates a friendly captcha.";
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

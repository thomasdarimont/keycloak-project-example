package com.github.thomasdarimont.keycloak.custom.auth.userpasswordform;

import com.github.thomasdarimont.keycloak.custom.security.friendlycaptcha.FriendlyCaptcha;
import com.github.thomasdarimont.keycloak.custom.support.LocaleUtils;
import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;

/**
 * UsernamePasswordForm with a friendlycaptcha
 */
public class AcmeCaptchaUsernamePasswordForm extends UsernamePasswordForm {

    public static final String ID = "acme-captcha-username-password-form";

    public static final String FRIENDLY_CAPTCHA_CHECK_TRIGGERED_AUTH_NOTE = "captchaTriggered";

    public static final String FRIENDLY_CAPTCHA_CHECK_SOLVED_AUTH_NOTE = "captchaSolved";

    @Override
    protected Response challenge(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        addCaptcha(context);
        return super.challenge(context, formData);
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, String error, String field) {
        addCaptcha(context);
        return super.challenge(context, error, field);
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, String error) {
        addCaptcha(context);
        return super.challenge(context, error);
    }

    private void addCaptcha(AuthenticationFlowContext context) {

        var captcha = new FriendlyCaptcha(context.getSession());
        if (!captcha.isEnabled()) {
            return;
        }

//            var realm = context.getRealm();
//            if (!realm.isBruteForceProtected()) {
//                return;
//            }
//
//            var attemptedUsername = context.getAuthenticationSession().getAuthNote(UsernamePasswordForm.ATTEMPTED_USERNAME);
//            if (attemptedUsername == null) {
//                return;
//            }
//
//            var session = context.getSession();
//            var user = session.users().getUserByUsername(realm, attemptedUsername);
//            if (user == null) {
//                return;
//            }
//
//            var userLoginFailures = session.loginFailures().getUserLoginFailure(realm, user.getId());
//            if (userLoginFailures == null) {
//                return;
//            }
//
//            // show friendly captcha only after 2-failed login attempts...
//            int maxNumFailuresForCaptcha = 2; // first attempt is not recorded, so existence of userLoginFailures counts as 1 therefor +1
//            if (userLoginFailures.getNumFailures() + 1 < maxNumFailuresForCaptcha) {
//                return;
//            }

        context.getAuthenticationSession().setAuthNote(FRIENDLY_CAPTCHA_CHECK_TRIGGERED_AUTH_NOTE, "true");

        var locale = LocaleUtils.extractLocaleWithFallbackToRealmLocale(context.getHttpRequest(), context.getRealm());
        captcha.configureForm(context.form(), locale);
    }

    @Override
    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {

        if (!checkCaptcha(context, formData)) {
            return false;
        }

        return super.validateForm(context, formData);
    }

    private boolean checkCaptcha(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {

        var session = context.getSession();
        var captcha = new FriendlyCaptcha(session);

        if (!captcha.isEnabled()) {
            return true;
        }

        var authSession = context.getAuthenticationSession();
        boolean captchaTriggered = Boolean.parseBoolean(authSession.getAuthNote(FRIENDLY_CAPTCHA_CHECK_TRIGGERED_AUTH_NOTE));
        if (!captchaTriggered) {
            return true;
        }

        var verificationResult = captcha.verifySolution(formData);
        if (!verificationResult.isSuccessful()) {
            context.getEvent().error("captcha-failed");
            var response = challenge(context, FriendlyCaptcha.FRIENDLY_CAPTCHA_SOLUTION_INVALID_MESSAGE, disabledByBruteForceFieldError());
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, response);
            return false;
        }

        authSession.removeAuthNote(FRIENDLY_CAPTCHA_CHECK_TRIGGERED_AUTH_NOTE);
        authSession.setAuthNote(FRIENDLY_CAPTCHA_CHECK_SOLVED_AUTH_NOTE, "true");

        return true;
    }


    @AutoService(AuthenticatorFactory.class)
    public static class Factory implements AuthenticatorFactory {

        private static final AcmeCaptchaUsernamePasswordForm INSTANCE = new AcmeCaptchaUsernamePasswordForm();

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
            return "Acme: Captcha Username Password Form";
        }

        @Override
        public String getHelpText() {
            return "Username Password Form with Captcha.";
        }

        @Override
        public String getReferenceCategory() {
            return "password";
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

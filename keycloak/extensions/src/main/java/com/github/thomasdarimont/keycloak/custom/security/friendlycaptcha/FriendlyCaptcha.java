package com.github.thomasdarimont.keycloak.custom.security.friendlycaptcha;

import jakarta.ws.rs.core.MultivaluedMap;
import lombok.Data;
import lombok.Getter;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;

import java.util.Locale;

/**
 * FriendlyCaptcha Facade
 */
@Getter
public class FriendlyCaptcha {

    public static final String FRIENDLY_CAPTCHA_SOLUTION_MISSING_MESSAGE = "friendly-captcha-solution-missing";

    public static final String FRIENDLY_CAPTCHA_SOLUTION_INVALID_MESSAGE = "friendly-captcha-solution-invalid";

    private final FriendlyCaptchaConfig config;

    private final FriendlyCaptchaClient client;

    public FriendlyCaptcha(KeycloakSession session, FriendlyCaptchaConfig config) {
        this.config = config;
        this.client = new FriendlyCaptchaClient(session, config);
    }

    public FriendlyCaptcha(KeycloakSession session) {
        this(session, new FriendlyCaptchaConfig(session.getContext().getRealm()));
    }


    public void configureForm(LoginFormsProvider form, Locale locale) {
        form.setAttribute("friendlyCaptchaEnabled", config.isEnabled());
        form.setAttribute("friendlyCaptchaSiteKey", config.getSiteKey());
        form.setAttribute("friendlyCaptchaStart", config.getStart());
        form.setAttribute("friendlyCaptchaLang", locale.getLanguage());
        form.setAttribute("friendlyCaptchaSourceModule", config.getSourceModule());
        form.setAttribute("friendlyCaptchaSourceNoModule", config.getSourceNoModule());
        form.setAttribute("friendlyCaptchaSolutionFieldName", config.getSolutionFieldName());
    }

    public boolean isEnabled() {
        return config.isEnabled();
    }

    public boolean verifySolution(String solutionValue) {
        return client.verifySolution(solutionValue);
    }

    public VerificationResult verifySolution(MultivaluedMap<String, String> formData) {

        var solutionFieldName = config.getSolutionFieldName();
        var solutionValue = formData.getFirst(solutionFieldName);
        if (solutionValue == null) {
            return new VerificationResult(false, FRIENDLY_CAPTCHA_SOLUTION_MISSING_MESSAGE);
        }

        var valid = verifySolution(solutionValue);
        if (!valid) {
            return new VerificationResult(false, FRIENDLY_CAPTCHA_SOLUTION_INVALID_MESSAGE);
        }

        return VerificationResult.OK;
    }

    @Data
    public static class VerificationResult {

        public static final VerificationResult OK = new VerificationResult(true, null);

        private final boolean successful;

        private final String errorMessage;
    }
}

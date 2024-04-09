package com.github.thomasdarimont.keycloak.custom.auth.verifyemailcode;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.Response;
import lombok.Data;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.protocol.AuthorizationEndpointBase;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@JBossLog
@AutoService(RequiredActionFactory.class)
public class VerifyEmailCodeAction implements RequiredActionProvider, RequiredActionFactory {

    public static final String PROVIDER_ID = "ACME_VERIFY_EMAIL_CODE";
    public static final String EMAIL_CODE_FORM = "email-code-form.ftl";
    public static final String EMAIL_CODE_NOTE = "emailCode";

    private VerifyEmailCodeActionConfig config;

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        if (context.getRealm().isVerifyEmail() && !context.getUser().isEmailVerified()) {
            context.getUser().addRequiredAction(PROVIDER_ID);
            log.debug("User is required to verify email");
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        requiredActionChallenge(context, null);
    }

    public void requiredActionChallenge(RequiredActionContext context, FormMessage errorMessage) {
        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        if (context.getUser().isEmailVerified()) {
            context.success();
            authSession.removeAuthNote(Constants.VERIFY_EMAIL_KEY);
            return;
        }

        String email = context.getUser().getEmail();
        if (Validation.isBlank(email)) {
            context.ignore();
            return;
        }

        LoginFormsProvider form = context.form();
        authSession.setClientNote(AuthorizationEndpointBase.APP_INITIATED_FLOW, null);

        // Do not allow resending e-mail by simple page refresh, i.e. when e-mail sent, it should be resent properly via email-verification endpoint
        if (!Objects.equals(authSession.getAuthNote(Constants.VERIFY_EMAIL_KEY), email)) {
            authSession.setAuthNote(Constants.VERIFY_EMAIL_KEY, email);
            EventBuilder event = context.getEvent().clone().event(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, email);
            generateAndSendEmailCode(context);
        }

        if (errorMessage != null) {
            form.setErrors(List.of(errorMessage));
        }

        form.setAttribute("codePattern", config.getCodePattern());
        form.setAttribute("codeLength", config.getCodeLengthUi());
        form.setAttribute("tryAutoSubmit", config.isTryAutoSubmit());

        Response challenge = form.createForm(EMAIL_CODE_FORM);

        context.challenge(challenge);
    }


    @Override
    public void processAction(RequiredActionContext context) {
        log.debugf("Re-sending email requested for user: %s", context.getUser().getUsername());

        // This will allow user to re-send email again
        context.getAuthenticationSession().removeAuthNote(Constants.VERIFY_EMAIL_KEY);

        var formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.containsKey("resend")) {
            resetEmailCode(context);
            requiredActionChallenge(context);
            return;
        }

        if (formData.containsKey("cancel")) {
            resetEmailCode(context);
            return;
        }

        var givenEmailCode = fromDisplayCode(formData.getFirst(EMAIL_CODE_NOTE));
        var valid = validateCode(context, givenEmailCode);
        // TODO add brute-force protection for email code auth

        context.getEvent().realm(context.getRealm()).user(context.getUser()).detail("action", PROVIDER_ID);

        if (!valid) {
            context.getEvent().event(EventType.VERIFY_EMAIL_ERROR).error(Errors.INVALID_USER_CREDENTIALS);
            requiredActionChallenge(context, new FormMessage(Messages.INVALID_ACCESS_CODE));
            return;
        }

        context.getUser().setEmailVerified(true);
        resetEmailCode(context);
        context.getEvent().event(EventType.VERIFY_EMAIL).success();
        context.success();
    }


    private void generateAndSendEmailCode(RequiredActionContext context) {

        if (context.getAuthenticationSession().getAuthNote(EMAIL_CODE_NOTE) != null) {
            // skip sending email code
            return;
        }

        var emailCode = SecretGenerator.getInstance().randomString(config.getCodeLength(), SecretGenerator.DIGITS);
        sendEmailWithCode(context, toDisplayCode(emailCode));

        context.getAuthenticationSession().setAuthNote(EMAIL_CODE_NOTE, emailCode);
    }

    private String toDisplayCode(String emailCode) {
        return new StringBuilder(emailCode).insert(config.getCodeLength() / 2, "-").toString();
    }

    private String fromDisplayCode(String code) {
        return code.replace("-", "");
    }

    private void resetEmailCode(RequiredActionContext context) {
        context.getAuthenticationSession().removeAuthNote(EMAIL_CODE_NOTE);
    }

    private boolean validateCode(RequiredActionContext context, String givenCode) {
        var emailCode = context.getAuthenticationSession().getAuthNote(EMAIL_CODE_NOTE);
        return emailCode.equals(givenCode);
    }

    private void sendEmailWithCode(RequiredActionContext context, String code) {

        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();
        KeycloakSession session = context.getSession();

        if (user.getEmail() == null) {
            log.warnf("Could not send access code email due to missing email. realm=%s user=%s", realm.getId(), user.getUsername());
            throw new AuthenticationFlowException(AuthenticationFlowError.INVALID_USER);
        }

        Map<String, Object> mailBodyAttributes = new HashMap<>();
        mailBodyAttributes.put("username", user.getUsername());
        mailBodyAttributes.put("code", code);


        var realmName = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();
        List<Object> subjectParams = List.of(realmName);

        try {
            var emailProvider = session.getProvider(EmailTemplateProvider.class);
            emailProvider.setRealm(realm);
            emailProvider.setUser(user);
            // Don't forget to add the code-email.ftl (html and text) template to your theme.
            emailProvider.send("emailCodeSubject", subjectParams, "code-email.ftl", mailBodyAttributes);
        } catch (EmailException eex) {
            log.errorf(eex, "Failed to send access code email. realm=%s user=%s", realm.getId(), user.getUsername());
        }
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayText() {
        return "Acme: Verify Email Code";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        this.config = new VerifyEmailCodeActionConfig(config);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {

    }

    @Data
    public static class VerifyEmailCodeActionConfig {

        private int codeLength;

        private int codeLengthUi;

        private String codePattern;

        private boolean tryAutoSubmit;

        public VerifyEmailCodeActionConfig(Config.Scope config) {
            this.codeLength = config.getInt("code-length", 8);
            this.codePattern = config.get("code-pattern", "\\d{4}-\\d{4}");
            this.codeLengthUi = config.getInt("code-length-ui", 8 + 1); //+1 for "-"
            this.tryAutoSubmit = config.getBoolean("try-auto-submit", false);
        }
    }
}

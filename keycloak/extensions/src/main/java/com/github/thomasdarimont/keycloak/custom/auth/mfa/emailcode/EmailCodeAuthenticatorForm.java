package com.github.thomasdarimont.keycloak.custom.auth.mfa.emailcode;

import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.CredentialValidator;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.messages.Messages;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JBossLog
public class EmailCodeAuthenticatorForm implements Authenticator, CredentialValidator<EmailCodeCredentialProvider> {

    static final String ID = "acme-email-code-form";

    public static final String EMAIL_CODE = "emailCode";
    public static final int LENGTH = 8;

    private final KeycloakSession session;

    public EmailCodeAuthenticatorForm(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        challenge(context, null);
    }

    private void challenge(AuthenticationFlowContext context, FormMessage errorMessage) {

        generateAndSendEmailCode(context);

        LoginFormsProvider form = context.form().setExecution(context.getExecution().getId());
        if (errorMessage != null) {
            form.setErrors(List.of(errorMessage));
        }

        form.setAttribute("codeLength", LENGTH + 1);
        form.setAttribute("tryAutoSubmit", true);
        form.setAttribute("codePattern", "\\d{4}-\\d{4}");

        Response response = form.createForm("email-code-form.ftl");

        context.challenge(response);
    }

    private void generateAndSendEmailCode(AuthenticationFlowContext context) {

        if (context.getAuthenticationSession().getAuthNote(EMAIL_CODE) != null) {
            // skip sending email code
            return;
        }

        var emailCode = SecretGenerator.getInstance().randomString(LENGTH, SecretGenerator.DIGITS);
        sendEmailWithCode(context.getRealm(), context.getUser(), toDisplayCode(emailCode));

        context.getAuthenticationSession().setAuthNote(EMAIL_CODE, emailCode);
    }

    private String toDisplayCode(String emailCode) {
        return new StringBuilder(emailCode).insert(LENGTH / 2, "-").toString();
    }

    private String fromDisplayCode(String code) {
        return code.replace("-", "");
    }

    @Override
    public void action(AuthenticationFlowContext context) {

        var formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.containsKey("resend")) {
            resetEmailCode(context);
            challenge(context, null);
            return;
        }

        if (formData.containsKey("cancel")) {
            resetEmailCode(context);
            context.resetFlow();
            return;
        }

        var givenEmailCode = fromDisplayCode(formData.getFirst(EMAIL_CODE));
        var valid = validateCode(context, givenEmailCode);
        // TODO add brute-force protection for email code auth

        context.getEvent().realm(context.getRealm()).user(context.getUser()).detail("authenticator", ID);

        if (!valid) {
            context.getEvent().event(EventType.LOGIN_ERROR).error(Errors.INVALID_USER_CREDENTIALS);
            challenge(context, new FormMessage(Messages.INVALID_ACCESS_CODE));
            return;
        }

        resetEmailCode(context);
        context.getEvent().event(EventType.LOGIN).success();
        context.success();
    }

    private void resetEmailCode(AuthenticationFlowContext context) {
        context.getAuthenticationSession().removeAuthNote(EMAIL_CODE);
    }

    private boolean validateCode(AuthenticationFlowContext context, String givenCode) {
        var emailCode = context.getAuthenticationSession().getAuthNote(EMAIL_CODE);
        return emailCode.equals(givenCode);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return user.credentialManager().isConfiguredFor(EmailCodeCredentialModel.TYPE);
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

    private void sendEmailWithCode(RealmModel realm, UserModel user, String code) {

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
    public EmailCodeCredentialProvider getCredentialProvider(KeycloakSession session) {
        // needed to access CredentialTypeMetadata for selecting authenticator options
        return (EmailCodeCredentialProvider)session.getProvider(CredentialProvider.class, EmailCodeCredentialProvider.ID);
    }

    @AutoService(AuthenticatorFactory.class)
    public static class Factory implements AuthenticatorFactory {

        @Override
        public String getDisplayType() {
            return "Acme: Email Code Form";
        }

        @Override
        public String getReferenceCategory() {
            return EmailCodeCredentialModel.TYPE;
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
        public String getHelpText() {
            return "Email code authenticator.";
        }

        @Override
        public List<ProviderConfigProperty> getConfigProperties() {
            return null;
        }

        @Override
        public void close() {
            // NOOP
        }

        @Override
        public Authenticator create(KeycloakSession session) {
            return new EmailCodeAuthenticatorForm(session);
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
        public String getId() {
            return EmailCodeAuthenticatorForm.ID;
        }
    }

}

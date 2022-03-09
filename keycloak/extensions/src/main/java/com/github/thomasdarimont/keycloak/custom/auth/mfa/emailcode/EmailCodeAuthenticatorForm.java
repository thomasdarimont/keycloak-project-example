package com.github.thomasdarimont.keycloak.custom.auth.mfa.emailcode;

import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JBossLog
public class EmailCodeAuthenticatorForm implements Authenticator {

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

        Response response = form.createForm("email-code-form.ftl");

        context.challenge(response);
    }

    private void generateAndSendEmailCode(AuthenticationFlowContext context) {

        if (context.getAuthenticationSession().getAuthNote(EMAIL_CODE) != null) {
            // skip sending email code
            return;
        }

        String emailCode = SecretGenerator.getInstance().randomString(LENGTH, SecretGenerator.DIGITS);
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

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();

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

        String givenEmailCode = fromDisplayCode(formData.getFirst(EMAIL_CODE));

        boolean valid = validateCode(context, givenEmailCode);


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
        String emailCode = context.getAuthenticationSession().getAuthNote(EMAIL_CODE);
        return emailCode.equals(givenCode);
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        // TODO determine if email code auth is configured for current user
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

    private void sendEmailWithCode(RealmModel realm, UserModel user, String code) {

        if (user.getEmail() == null) {
            log.warnf("Could not send access code email due to missing email. realm=%s user=%s", realm.getId(), user.getUsername());
            throw new AuthenticationFlowException(AuthenticationFlowError.INVALID_USER);
        }

        Map<String, Object> mailBodyAttributes = new HashMap<>();
        mailBodyAttributes.put("username", user.getUsername());
        mailBodyAttributes.put("code", code);


        String realmName = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();
        List<Object> subjectParams = List.of(realmName);

        try {
            EmailTemplateProvider emailProvider = session.getProvider(EmailTemplateProvider.class);
            emailProvider.setRealm(realm);
            emailProvider.setUser(user);
            // Don't forget to add the welcome-email.ftl (html and text) template to your theme.
            emailProvider.send("emailCodeSubject", subjectParams, "code-email.ftl", mailBodyAttributes);
        } catch (EmailException eex) {
            log.errorf(eex, "Failed to send access code email. realm=%s user=%s", realm.getId(), user.getUsername());
        }
    }
}

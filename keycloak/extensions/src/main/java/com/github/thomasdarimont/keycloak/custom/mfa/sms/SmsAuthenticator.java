package com.github.thomasdarimont.keycloak.custom.mfa.sms;

import com.github.thomasdarimont.keycloak.custom.mfa.sms.client.SmsClient;
import com.github.thomasdarimont.keycloak.custom.mfa.sms.client.SmsClientFactory;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.RandomString;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.representations.IDToken;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.Theme;

import javax.ws.rs.core.Response;
import java.net.URI;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@JBossLog
public class SmsAuthenticator implements Authenticator {

    static final String TEMPLATE_LOGIN_SMS = "login-sms.ftl";

    static final String CONFIG_CODE_LENGTH = "length";
    static final String CONFIG_MAX_ATTEMPTS = "attempts";
    static final String CONFIG_CODE_TTL = "ttl";
    static final String CONFIG_SENDER = "sender";
    static final String CONFIG_CLIENT = "client";
    static final String CONFIG_PHONENUMBER_PATTERN = "phoneNumberPattern";

    static final String AUTH_NOTE_CODE = "smsCode";
    static final String AUTH_NOTE_ATTEMPTS = "smsAttempts";

    static final String ERROR_SMS_AUTH_INVALID_NUMBER = "smsAuthInvalidNumber";
    static final String ERROR_SMS_AUTH_CODE_EXPIRED = "smsAuthCodeExpired";
    static final String ERROR_SMS_AUTH_CODE_INVALID = "smsAuthCodeInvalid";
    static final String ERROR_SMS_AUTH_SMS_NOT_SENT = "smsAuthSmsNotSent";
    static final String ERROR_SMS_AUTH_ATTEMPTS_EXCEEDED = "smsAuthAttemptsExceeded";

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        if (context.getAuthenticationSession().getAuthNote(AUTH_NOTE_CODE) != null) {
            // avoid sending resending code on reload
            context.challenge(generateLoginForm(context, context.form()).createForm(TEMPLATE_LOGIN_SMS));
            return;
        }

        UserModel user = context.getUser();
        String phoneNumber = extractPhoneNumber(user);
        boolean validPhoneNumberFormat = validatePhoneNumberFormat(phoneNumber, context);
        if (!validPhoneNumberFormat) {
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    generateErrorForm(context, ERROR_SMS_AUTH_INVALID_NUMBER)
                            .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
            return;
        }

        String phoneNumberVerified = user.getFirstAttribute(IDToken.PHONE_NUMBER_VERIFIED);
        // TODO check for phoneNumberVerified

        sendCodeAndChallenge(context, user, phoneNumber, false);
    }

    protected String extractPhoneNumber(UserModel user) {
        return user.getFirstAttribute(IDToken.PHONE_NUMBER);
    }

    protected void sendCodeAndChallenge(AuthenticationFlowContext context, UserModel user, String phoneNumber, boolean resend) {
        log.infof("Sending code via SMS. resend=%s", resend);

        boolean codeSent = sendSmsWithCode(context, user, phoneNumber);

        if (!codeSent) {
            Response errorPage = generateErrorForm(context, null)
                    .setError(ERROR_SMS_AUTH_SMS_NOT_SENT, "Sms Client")
                    .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR);
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR, errorPage);
            return;
        }

        context.challenge(generateLoginForm(context, context.form())
                .setAttribute("resend", resend)
                .setInfo("smsSentInfo")
                .createForm(TEMPLATE_LOGIN_SMS));
    }

    protected LoginFormsProvider generateLoginForm(AuthenticationFlowContext context, LoginFormsProvider form) {
        return form.setAttribute("realm", context.getRealm());
    }

    protected boolean sendSmsWithCode(AuthenticationFlowContext context, UserModel user, String phoneNumber) {

        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        int length = Integer.parseInt(getConfigValue(context, CONFIG_CODE_LENGTH, "6"));
        int ttl = Integer.parseInt(getConfigValue(context, CONFIG_CODE_TTL, "300"));

        String code = generateCode(length);
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        authSession.setAuthNote(AUTH_NOTE_CODE, code);
        authSession.setAuthNote("codeExpireAt", computeExpireAt(ttl));

        try {
            KeycloakSession session = context.getSession();
            Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
            Locale locale = session.getContext().resolveLocale(user);
            String smsAuthText = theme.getMessages(locale).getProperty("smsAuthText");
            String boundDomain = resolveRealmDomain(context);
            String smsText = generateSmsText(ttl, code, smsAuthText, boundDomain);

            SmsClient smsClient = createSmsClient(config.getConfig());

            String sender = resolveSender(context);
            smsClient.send(sender, phoneNumber, smsText);

        } catch (Exception e) {
            log.errorf(e, "Could not send sms");
            return false;
        }

        return true;
    }

    private String generateSmsText(int ttlSeconds, String code, String smsAuthText, String boundDomain) {
        int ttlMinutes = Math.floorDiv(ttlSeconds, 60);
        return String.format(smsAuthText, code, ttlMinutes, boundDomain);
    }

    private String computeExpireAt(int ttlSeconds) {
        return Long.toString(System.currentTimeMillis() + (ttlSeconds * 1000));
    }

    protected String getConfigValue(AuthenticationFlowContext context, String key, String defaultValue) {

        AuthenticatorConfigModel configModel = context.getAuthenticatorConfig();
        if (configModel == null) {
            return defaultValue;
        }

        Map<String, String> config = configModel.getConfig();
        if (config == null) {
            return defaultValue;
        }

        return config.getOrDefault(key, defaultValue);
    }

    protected String resolveRealmDomain(AuthenticationFlowContext context) {
        return URI.create(System.getenv("KEYCLOAK_FRONTEND_URL")).getHost();
    }

    protected SmsClient createSmsClient(Map<String, String> config) {
        String smsClientName = config.get(CONFIG_CLIENT);
        return SmsClientFactory.createClient(smsClientName, config);
    }

    protected String resolveSender(AuthenticationFlowContext context) {

        RealmModel realm = context.getRealm();
        String sender = getConfigValue(context, "sender", "keycloak");
        if ("$realmDisplayName".equals(sender.trim())) {
            sender = realm.getDisplayName();
        }
        return sender;
    }

    protected boolean validatePhoneNumberFormat(String phoneNumber, AuthenticationFlowContext context) {

        if (phoneNumber == null) {
            return false;
        }

        String pattern = getConfigValue(context, CONFIG_PHONENUMBER_PATTERN, ".*");
        return phoneNumber.matches(pattern);
    }

    protected String generateCode(int length) {
        return new RandomString(length, new SecureRandom(), RandomString.digits).nextString();
    }

    @Override
    public void action(AuthenticationFlowContext context) {

        var formParams = context.getHttpRequest().getDecodedFormParameters();

        if (formParams.containsKey("resend")) {
            UserModel user = context.getUser();
            String phoneNumber = extractPhoneNumber(user);
            sendCodeAndChallenge(context, user, phoneNumber, true);
            return;
        }

        String codeInput = formParams.getFirst("code");

        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        int attempts = Integer.parseInt(Optional.ofNullable(authSession.getAuthNote(AUTH_NOTE_ATTEMPTS)).orElse("0"));
        int maxAttempts = Integer.parseInt(getConfigValue(context, CONFIG_MAX_ATTEMPTS, "5"));
        if (attempts >= maxAttempts) {
            log.info("To many invalid attempts.");
            Response errorPage = generateErrorForm(context, ERROR_SMS_AUTH_ATTEMPTS_EXCEEDED)
                    .createErrorPage(Response.Status.BAD_REQUEST);
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS, errorPage);
            return;
        }

        String codeExpected = authSession.getAuthNote(AUTH_NOTE_CODE);
        String codeExpireAt = authSession.getAuthNote("codeExpireAt");

        if (codeExpected == null || codeExpireAt == null) {
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
            return;
        }

        boolean valid = codeInput.equals(codeExpected);
        if (!valid) {
            Response errorPage = generateErrorForm(context, null)
                    .setErrors(List.of(new FormMessage("code", ERROR_SMS_AUTH_CODE_INVALID)))
                    .setAttribute("showResend", "")
                    .createForm(TEMPLATE_LOGIN_SMS);
            handleFailure(context, AuthenticationFlowError.INVALID_CREDENTIALS, errorPage);
            return;
        }

        if (isCodeExpired(codeExpireAt)) {
            Response errorPage = generateErrorForm(context, null)
                    .setErrors(List.of(new FormMessage("code", ERROR_SMS_AUTH_CODE_EXPIRED)))
                    .setAttribute("showResend", "")
                    .createErrorPage(Response.Status.BAD_REQUEST);
            handleFailure(context, AuthenticationFlowError.EXPIRED_CODE, errorPage);
            return;
        }

        context.success();
    }

    protected void handleFailure(AuthenticationFlowContext context, AuthenticationFlowError error, Response errorPage) {

        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        int attempts = Integer.parseInt(Optional.ofNullable(authSession.getAuthNote(AUTH_NOTE_ATTEMPTS)).orElse("0"));
        attempts++;
        authSession.setAuthNote(AUTH_NOTE_ATTEMPTS, "" + attempts);

        context.failureChallenge(error, errorPage);
    }

    protected boolean isCodeExpired(String codeExpireAt) {
        return Long.parseLong(codeExpireAt) < System.currentTimeMillis();
    }

    protected LoginFormsProvider generateErrorForm(AuthenticationFlowContext context, String error) {

        LoginFormsProvider form = context.form();
        generateLoginForm(context, form);

        if (error == null) {
            form.setError(error);
        }

        return form;
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        // we only support 2FA with SMS for users with Phone Numbers
        return extractPhoneNumber(user) != null;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
        // NOOP
    }

    @Override
    public void close() {
        // NOOP
    }

}
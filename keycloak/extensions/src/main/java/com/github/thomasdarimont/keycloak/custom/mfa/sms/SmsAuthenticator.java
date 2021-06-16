package com.github.thomasdarimont.keycloak.custom.mfa.sms;

import com.github.thomasdarimont.keycloak.custom.mfa.sms.client.SmsClient;
import com.github.thomasdarimont.keycloak.custom.mfa.sms.client.SmsClientFactory;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.RandomString;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.IDToken;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.Theme;

import javax.ws.rs.core.Response;
import java.util.Locale;

public class SmsAuthenticator implements Authenticator {

    private static final String TEMPLATE_LOGIN_SMS = "login-sms.ftl";

    static final String CONFIG_CODE_LENGTH = "length";

    static final String CONFIG_CODE_TTL = "ttl";

    static final String CONFIG_SENDER = "sender";

    static final String CONFIG_CLIENT = "client";

    // TODO add configurable attempts

    public static final String ERROR_SMS_AUTH_INVALID_NUMBER = "smsAuthInvalidNumber";
    public static final String ERROR_SMS_AUTH_CODE_EXPIRED = "smsAuthCodeExpired";
    public static final String ERROR_SMS_AUTH_CODE_INVALID = "smsAuthCodeInvalid";
    public static final String ERROR_SMS_AUTH_SMS_NOT_SENT = "smsAuthSmsNotSent";

    @Override
    public void authenticate(AuthenticationFlowContext context) {

        UserModel user = context.getUser();

        String phoneNumber = user.getFirstAttribute(IDToken.PHONE_NUMBER);
        boolean validPhoneNumberFormat = validatePhoneNumberFormat(phoneNumber);
        if (!validPhoneNumberFormat) {
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    generateErrorForm(context, ERROR_SMS_AUTH_INVALID_NUMBER)
                            .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
            return;
        }

        String phoneNumberVerified = user.getFirstAttribute(IDToken.PHONE_NUMBER_VERIFIED);
        // TODO check for phoneNumberVerified

        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        int length = Integer.parseInt(config.getConfig().get(CONFIG_CODE_LENGTH));
        int ttl = Integer.parseInt(config.getConfig().get(CONFIG_CODE_TTL));

        String code = generateCode(length);
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        authSession.setAuthNote("code", code);
        authSession.setAuthNote("codeExpireAt", Long.toString(System.currentTimeMillis() + (ttl * 1000)));

        try {
            KeycloakSession session = context.getSession();
            Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
            Locale locale = session.getContext().resolveLocale(user);
            String smsAuthText = theme.getMessages(locale).getProperty("smsAuthText");
            String smsText = String.format(smsAuthText, code, Math.floorDiv(ttl, 60));

            SmsClient smsClient = createSmsClient(config);

            String sender = resolveSender(context, config);
            smsClient.send(sender, phoneNumber, smsText);

            context.challenge(context.form().setAttribute("realm", context.getRealm()).createForm(TEMPLATE_LOGIN_SMS));
        } catch (Exception e) {
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    generateErrorForm(context, null).setError(ERROR_SMS_AUTH_SMS_NOT_SENT, e.getMessage())
                            .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
        }
    }

    protected SmsClient createSmsClient(AuthenticatorConfigModel config) {
        String smsClientName = config.getConfig().get(CONFIG_CLIENT);
        return SmsClientFactory.createClient(smsClientName, config.getConfig());
    }

    protected String resolveSender(AuthenticationFlowContext context, AuthenticatorConfigModel config) {

        RealmModel realm = context.getRealm();
        String sender = config.getConfig().getOrDefault("sender", "keycloak");
        if ("$realmDisplayName".equals(sender.trim())) {
            sender = realm.getDisplayName();
        }
        return sender;
    }

    protected boolean validatePhoneNumberFormat(String phoneNumber) {
        // TODO validate phoneNumber
        return true;
    }

    protected String generateCode(int length) {
        return RandomString.randomCode(length);
    }

    @Override
    public void action(AuthenticationFlowContext context) {

        String codeInput = context.getHttpRequest().getDecodedFormParameters().getFirst("code");

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        String codeExpected = authSession.getAuthNote("code");
        String codeExpireAt = authSession.getAuthNote("codeExpireAt");

        if (codeExpected == null || codeExpireAt == null) {
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                    context.form().createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
            return;
        }

        boolean isValid = codeInput.equals(codeExpected);
        if (!isValid) {
            context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                    generateErrorForm(context, null)
                            .setError("code", ERROR_SMS_AUTH_CODE_INVALID)
                            .createForm(TEMPLATE_LOGIN_SMS));
            return;
        }

        if (isCodeExpired(codeExpireAt)) {
            Response errorPage = generateErrorForm(context, null)
                    .setError("code", ERROR_SMS_AUTH_CODE_EXPIRED)
                    .createErrorPage(Response.Status.BAD_REQUEST);
            context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE, errorPage);
            return;
        }

        context.success();
    }

    private boolean isCodeExpired(String codeExpireAt) {
        return Long.parseLong(codeExpireAt) < System.currentTimeMillis();
    }

    private LoginFormsProvider generateErrorForm(AuthenticationFlowContext context, String error) {

        LoginFormsProvider form = context.form();
        form.setAttribute("realm", context.getRealm());

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
        return user.getFirstAttribute(IDToken.PHONE_NUMBER) != null;
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
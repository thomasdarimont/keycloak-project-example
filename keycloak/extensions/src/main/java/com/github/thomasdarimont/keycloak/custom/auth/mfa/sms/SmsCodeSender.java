package com.github.thomasdarimont.keycloak.custom.auth.mfa.sms;

import com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.client.SmsClient;
import com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.client.SmsClientFactory;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.common.util.RandomString;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakUriInfo;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.Theme;
import org.keycloak.urls.UrlType;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;

@JBossLog
public class SmsCodeSender {

    public boolean sendVerificationCode(KeycloakSession session, RealmModel realm, UserModel user, String phoneNumber,
                                        Map<String, String> smsClientConfig, int codeLength, int codeTtl, boolean useWebOtp,
                                        AuthenticationSessionModel authSession) {

        String code = generateCode(codeLength);
        authSession.setAuthNote(SmsAuthenticator.AUTH_NOTE_CODE, code);
        authSession.setAuthNote("codeExpireAt", computeExpireAt(codeTtl));

        KeycloakContext context = session.getContext();
        String domain = resolveDomain(context);
        String sender = resolveSender(realm, smsClientConfig);

        try {
            Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
            Locale locale = context.resolveLocale(user);
            String smsAuthText = theme.getMessages(locale).getProperty("smsAuthText");
            String smsText = generateSmsText(codeTtl, code, smsAuthText, domain, useWebOtp);
            SmsClient smsClient = createSmsClient(smsClientConfig);
            smsClient.send(sender, phoneNumber, smsText);
        } catch (Exception e) {
            log.errorf(e, "Could not send sms");
            return false;
        }

        return true;
    }

    protected String generateCode(int length) {
        return new RandomString(length, new SecureRandom(), RandomString.digits).nextString();
    }

    protected String resolveDomain(KeycloakContext context) {
        KeycloakUriInfo uri = context.getUri(UrlType.FRONTEND);
        return uri.getBaseUri().getHost();
    }

    protected SmsClient createSmsClient(Map<String, String> config) {
        String smsClientName = config.get(SmsAuthenticator.CONFIG_CLIENT);
        return SmsClientFactory.createClient(smsClientName, config);
    }

    protected String generateSmsText(int ttlSeconds, String code, String smsAuthText, String domain, boolean useWebOtp) {
        int ttlMinutes = Math.floorDiv(ttlSeconds, 60);
        String smsAuthMessage = String.format(smsAuthText, code, ttlMinutes);
        if (!useWebOtp) {
            return smsAuthMessage;
        }
        return appendWebOtpFragment(code, domain, smsAuthMessage);
    }

    protected String appendWebOtpFragment(String code, String domain, String smsAuthFragment) {
        String webOtpFragment = String.format("@%s #%s", domain, code);
        return smsAuthFragment + "\n" + webOtpFragment;
    }

    protected String computeExpireAt(int ttlSeconds) {
        return Long.toString(System.currentTimeMillis() + (ttlSeconds * 1000L));
    }

    protected String resolveSender(RealmModel realm, Map<String, String> clientConfig) {

        String sender = clientConfig.getOrDefault("sender", "keycloak");
        if ("$realmDisplayName".equals(sender.trim())) {
            sender = realm.getDisplayName();
        }
        return sender;
    }

}

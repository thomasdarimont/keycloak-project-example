package com.github.thomasdarimont.keycloak.custom.mfa.sms;

import com.github.thomasdarimont.keycloak.custom.mfa.sms.client.SmsClient;
import com.github.thomasdarimont.keycloak.custom.mfa.sms.client.SmsClientFactory;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.common.util.RandomString;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.theme.Theme;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Map;

@JBossLog
public class SmsCodeSender {

    protected String generateCode(int length) {
        return new RandomString(length, new SecureRandom(), RandomString.digits).nextString();
    }

    public boolean sendVerificationCode(KeycloakSession session, RealmModel realm, UserModel user, String phoneNumber,
                                        Map<String, String> smsClientConfig, int codeLength, int codeTtl,
                                        AuthenticationSessionModel authSession) {

        String code = generateCode(codeLength);
        authSession.setAuthNote(SmsAuthenticator.AUTH_NOTE_CODE, code);
        authSession.setAuthNote("codeExpireAt", computeExpireAt(codeTtl));

        String boundDomain = resolveRealmDomain(realm);
        String sender = resolveSender(realm, smsClientConfig);

        try {
            Theme theme = session.theme().getTheme(Theme.Type.LOGIN);
            Locale locale = session.getContext().resolveLocale(user);
            String smsAuthText = theme.getMessages(locale).getProperty("smsAuthText");
            String smsText = generateSmsText(codeTtl, code, smsAuthText, boundDomain);
            SmsClient smsClient = createSmsClient(smsClientConfig);
            smsClient.send(sender, phoneNumber, smsText);
        } catch (Exception e) {
            log.errorf(e, "Could not send sms");
            return false;
        }

        return true;
    }

    protected String resolveRealmDomain(RealmModel context) {
        return URI.create(System.getenv("KEYCLOAK_FRONTEND_URL")).getHost();
    }

    protected SmsClient createSmsClient(Map<String, String> config) {
        String smsClientName = config.get(SmsAuthenticator.CONFIG_CLIENT);
        return SmsClientFactory.createClient(smsClientName, config);
    }

    private String generateSmsText(int ttlSeconds, String code, String smsAuthText, String boundDomain) {
        int ttlMinutes = Math.floorDiv(ttlSeconds, 60);
        return String.format(smsAuthText, code, ttlMinutes, boundDomain);
    }

    private String computeExpireAt(int ttlSeconds) {
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

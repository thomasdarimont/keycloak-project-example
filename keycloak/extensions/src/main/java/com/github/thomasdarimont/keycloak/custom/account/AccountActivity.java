package com.github.thomasdarimont.keycloak.custom.account;

import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.action.TrustedDeviceInfo;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JBossLog
public class AccountActivity {

    enum TrustedDeviceChange {
        ADD,
        REMOVE
    }

    public static void onTrustedDeviceAdded(KeycloakSession session,  //
                                            RealmModel realm,  //
                                            UserModel user,  //
                                            TrustedDeviceInfo trustedDeviceInfo) {

        notifyAboutTrustedDeviceChange(session, realm, user, trustedDeviceInfo, TrustedDeviceChange.ADD);
    }

    public static void onTrustedDeviceRemoved(KeycloakSession session,  //
                                              RealmModel realm,  //
                                              UserModel user,  //
                                              TrustedDeviceInfo trustedDeviceInfo) {

        notifyAboutTrustedDeviceChange(session, realm, user, trustedDeviceInfo, TrustedDeviceChange.REMOVE);
    }

    private static void notifyAboutTrustedDeviceChange(KeycloakSession session, RealmModel realm, UserModel user, TrustedDeviceInfo trustedDeviceInfo, TrustedDeviceChange change) {

        EmailTemplateProvider emailTemplateProvider = session.getProvider(EmailTemplateProvider.class);
        emailTemplateProvider.setRealm(realm);
        emailTemplateProvider.setUser(user);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("trustedDeviceInfo", trustedDeviceInfo);
        attributes.put("user", new ProfileBean(user));

        String realmDisplayName = realm.getDisplayName();
        if (realmDisplayName == null) {
            realmDisplayName = realm.getName();
        }
        try {
            switch (change) {
                case ADD:
                    emailTemplateProvider.send("acmeTrustedDeviceAddedSubject", List.of(realmDisplayName),
                            "acme-trusted-device-added.ftl", attributes);
                    break;
                case REMOVE:
                    emailTemplateProvider.send("acmeTrustedDeviceRemovedSubject", List.of(realmDisplayName),
                            "acme-trusted-device-removed.ftl", attributes);
                    break;
                default:
                    break;
            }


        } catch (EmailException e) {
            log.errorf(e, "Failed to send email for new trusted device.");
        }
    }
}

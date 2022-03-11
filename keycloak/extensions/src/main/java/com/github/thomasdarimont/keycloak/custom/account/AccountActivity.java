package com.github.thomasdarimont.keycloak.custom.account;

import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.action.TrustedDeviceInfo;
import com.github.thomasdarimont.keycloak.custom.themes.login.AcmeUrlBean;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.common.util.Time;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.resources.LoginActionsService;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JBossLog
public class AccountActivity {

    enum TrustedDeviceChange {
        ADD, REMOVE
    }

    public static void onAccountDeletionRequested(KeycloakSession session, RealmModel realm, UserModel user, UriInfo uriInfo) {
        var realmDisplayName = getRealmDisplayName(realm);
        try {
            URI actionTokenUrl = createAccountDeletionActionToken(session, realm, user, uriInfo);
            sendEmail(session, realm, user, (emailTemplateProvider, attributes) -> {
                attributes.put("actionTokenUrl", actionTokenUrl);
                emailTemplateProvider.send("acmeAccountDeletionRequestedSubject", List.of(realmDisplayName), "acme-account-deletion-requested.ftl", attributes);
            });
            log.infof("Requested user account deletion. realm=%s userId=%s", realm.getName(), user.getId());
        } catch (EmailException e) {
            log.errorf(e, "Failed to send email for account deletion request.");
        }
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
        try {
            var realmDisplayName = getRealmDisplayName(realm);

            switch (change) {
                case ADD:
                    sendEmail(session, realm, user, (emailTemplateProvider, attributes) -> {
                        attributes.put("trustedDeviceInfo", trustedDeviceInfo);
                        emailTemplateProvider.send("acmeTrustedDeviceAddedSubject", List.of(realmDisplayName), "acme-trusted-device-added.ftl", attributes);
                    });
                    break;
                case REMOVE:
                    sendEmail(session, realm, user, (emailTemplateProvider, attributes) -> {
                        attributes.put("trustedDeviceInfo", trustedDeviceInfo);
                        emailTemplateProvider.send("acmeTrustedDeviceRemovedSubject", List.of(realmDisplayName), "acme-trusted-device-removed.ftl", attributes);
                    });
                    break;
                default:
                    break;
            }
        } catch (EmailException e) {
            log.errorf(e, "Failed to send email for new trusted device.");
        }
    }


    private static void sendEmail(KeycloakSession session, RealmModel realm, UserModel user, SendEmailTask sendEmailTask) throws EmailException {
        EmailTemplateProvider emailTemplateProvider = session.getProvider(EmailTemplateProvider.class);
        emailTemplateProvider.setRealm(realm);
        emailTemplateProvider.setUser(user);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("user", new ProfileBean(user));

        sendEmailTask.sendEmail(emailTemplateProvider, attributes);
    }

    private static String getRealmDisplayName(RealmModel realm) {
        String realmDisplayName = realm.getDisplayName();
        if (realmDisplayName == null) {
            realmDisplayName = realm.getName();
        }
        return realmDisplayName;
    }


    private static URI createAccountDeletionActionToken(KeycloakSession session, RealmModel realm, UserModel user, UriInfo uriInfo) {
        String userId = user.getId();
        int validityInSecs = realm.getActionTokenGeneratedByAdminLifespan();
        int absoluteExpirationInSecs = Time.currentTime() + validityInSecs;
        RequestAccountDeletionActionToken requestAccountDeletionActionToken = new RequestAccountDeletionActionToken(userId, absoluteExpirationInSecs, Constants.ACCOUNT_MANAGEMENT_CLIENT_ID, new AcmeUrlBean(session).getAccountDeletedUrl());
        String token = requestAccountDeletionActionToken.serialize(session, realm, uriInfo);
        UriBuilder builder = LoginActionsService.actionTokenProcessor(session.getContext().getUri());
        builder.queryParam("key", token);
        String actionTokenLink = builder.build(realm.getName()).toString();
        return URI.create(actionTokenLink);
    }

    @FunctionalInterface
    interface SendEmailTask {

        void sendEmail(EmailTemplateProvider emailTemplateProvider, Map<String, Object> attributes) throws EmailException;
    }
}

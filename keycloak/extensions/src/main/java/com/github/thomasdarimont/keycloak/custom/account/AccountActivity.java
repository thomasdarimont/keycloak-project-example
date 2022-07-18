package com.github.thomasdarimont.keycloak.custom.account;

import com.github.thomasdarimont.keycloak.custom.auth.mfa.MfaInfo;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.action.TrustedDeviceInfo;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.credential.CredentialModel;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;

import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;

@JBossLog
public class AccountActivity {

    public static void onUserMfaChanged(KeycloakSession session, RealmModel realm, UserModel user, CredentialModel credential, MfaChange change) {

        try {
            var realmDisplayName = getRealmDisplayName(realm);
            var credentialLabel = getCredentialLabel(credential);
            var mfaInfo = new MfaInfo(credentialLabel);
            switch (change) {
                case ADD:
                    AccountEmail.send(session.getProvider(EmailTemplateProvider.class), realm, user, (emailTemplateProvider, attributes) -> {
                        attributes.put("mfaInfo", mfaInfo);
                        emailTemplateProvider.send("acmeMfaAddedSubject", List.of(realmDisplayName), "acme-mfa-added.ftl", attributes);
                    });
                    break;
                case REMOVE:
                    AccountEmail.send(session.getProvider(EmailTemplateProvider.class), realm, user, (emailTemplateProvider, attributes) -> {
                        attributes.put("mfaInfo", mfaInfo);
                        emailTemplateProvider.send("acmeMfaRemovedSubject", List.of(realmDisplayName), "acme-mfa-removed.ftl", attributes);
                    });
                    break;
                default:
                    break;
            }
        } catch (EmailException e) {
            log.errorf(e, "Failed to send email for new user mfa change: %s.", change);
        }
    }

    public static void onAccountDeletionRequested(KeycloakSession session, RealmModel realm, UserModel user, UriInfo uriInfo) {
        var realmDisplayName = getRealmDisplayName(realm);
        try {
            URI actionTokenUrl = AccountDeletion.createActionToken(session, realm, user, uriInfo);
            AccountEmail.send(session.getProvider(EmailTemplateProvider.class), realm, user, (emailTemplateProvider, attributes) -> {
                attributes.put("actionTokenUrl", actionTokenUrl);
                emailTemplateProvider.send("acmeAccountDeletionRequestedSubject", List.of(realmDisplayName), "acme-account-deletion-requested.ftl", attributes);
            });
            log.infof("Requested user account deletion. realm=%s userId=%s", realm.getName(), user.getId());
        } catch (EmailException e) {
            log.errorf(e, "Failed to send email for account deletion request.");
        }
    }

    public static void onTrustedDeviceChange(KeycloakSession session, RealmModel realm, UserModel user, TrustedDeviceInfo trustedDeviceInfo, MfaChange change) {
        try {
            var realmDisplayName = getRealmDisplayName(realm);

            switch (change) {
                case ADD:
                    AccountEmail.send(session.getProvider(EmailTemplateProvider.class), realm, user, (emailTemplateProvider, attributes) -> {
                        attributes.put("trustedDeviceInfo", trustedDeviceInfo);
                        emailTemplateProvider.send("acmeTrustedDeviceAddedSubject", List.of(realmDisplayName), "acme-trusted-device-added.ftl", attributes);
                    });
                    break;
                case REMOVE:
                    AccountEmail.send(session.getProvider(EmailTemplateProvider.class), realm, user, (emailTemplateProvider, attributes) -> {
                        attributes.put("trustedDeviceInfo", trustedDeviceInfo);
                        emailTemplateProvider.send("acmeTrustedDeviceRemovedSubject", List.of(realmDisplayName), "acme-trusted-device-removed.ftl", attributes);
                    });
                    break;
                default:
                    break;
            }
        } catch (EmailException e) {
            log.errorf(e, "Failed to send email for trusted device change: %s.", change);
        }
    }

    public static void onAccountLockedOut(KeycloakSession session, RealmModel realm, UserModel user, UserLoginFailureModel userLoginFailure) {
        var realmDisplayName = getRealmDisplayName(realm);
        try {
            AccountEmail.send(session.getProvider(EmailTemplateProvider.class), realm, user, (emailTemplateProvider, attributes) -> {
                attributes.put("userLoginFailure", userLoginFailure);
                emailTemplateProvider.send("acmeAccountBlockedSubject", List.of(realmDisplayName), "acme-account-blocked.ftl", attributes);
            });
        } catch (EmailException e) {
            log.errorf(e, "Failed to send email for user account block. userId=%s", userLoginFailure.getUserId());
        }
    }

    private static String getRealmDisplayName(RealmModel realm) {
        var realmDisplayName = realm.getDisplayName();
        if (realmDisplayName == null) {
            realmDisplayName = realm.getName();
        }
        return realmDisplayName;
    }

    private static String toReadableCredentialType(CredentialModel credential) {
        if (OTPCredentialModel.TYPE.equals(credential.getType())) {
            return credential.getType().toUpperCase();
        }
        return credential.getType();
    }

    private static String getCredentialLabel(CredentialModel credential) {
        var label = credential.getUserLabel();
        var type = credential.getType();

        if (label != null && type != null) {
            return label + " " + toReadableCredentialType(credential);
        }

        if (label != null) {
            return label;
        }

        return toReadableCredentialType(credential);
    }
}

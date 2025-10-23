package com.github.thomasdarimont.keycloak.custom.account;

import com.github.thomasdarimont.keycloak.custom.auth.mfa.MfaInfo;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.action.TrustedDeviceInfo;
import com.github.thomasdarimont.keycloak.custom.support.RealmUtils;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.credential.CredentialModel;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;

import java.net.URI;
import java.util.List;

@JBossLog
public class AccountActivity {

    public static void onUserMfaChanged(KeycloakSession session, RealmModel realm, UserModel user, CredentialModel credential, MfaChange change) {

        try {
            var credentialLabel = getCredentialLabel(credential);
            var mfaInfo = new MfaInfo(credential.getType(), credentialLabel);
            switch (change) {
                case ADD:
                    AccountEmail.send(session, session.getProvider(EmailTemplateProvider.class), realm, user, (emailTemplateProvider, attributes) -> {
                        attributes.put("mfaInfo", mfaInfo);
                        emailTemplateProvider.send("acmeMfaAddedSubject", List.of(RealmUtils.getDisplayName(realm)), "acme-mfa-added.ftl", attributes);
                    });
                    break;
                case REMOVE:
                    AccountEmail.send(session, session.getProvider(EmailTemplateProvider.class), realm, user, (emailTemplateProvider, attributes) -> {
                        attributes.put("mfaInfo", mfaInfo);
                        emailTemplateProvider.send("acmeMfaRemovedSubject", List.of(RealmUtils.getDisplayName(realm)), "acme-mfa-removed.ftl", attributes);
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
        try {
            URI actionTokenUrl = AccountDeletion.createActionToken(session, realm, user, uriInfo);
            AccountEmail.send(session, session.getProvider(EmailTemplateProvider.class), realm, user, (emailTemplateProvider, attributes) -> {
                attributes.put("actionTokenUrl", actionTokenUrl);
                emailTemplateProvider.send("acmeAccountDeletionRequestedSubject", List.of(RealmUtils.getDisplayName(realm)), "acme-account-deletion-requested.ftl", attributes);
            });
            log.infof("Requested user account deletion. realm=%s userId=%s", realm.getName(), user.getId());
        } catch (EmailException e) {
            log.errorf(e, "Failed to send email for account deletion request.");
        }
    }

    public static void onTrustedDeviceChange(KeycloakSession session, RealmModel realm, UserModel user, TrustedDeviceInfo trustedDeviceInfo, MfaChange change) {
        try {

            switch (change) {
                case ADD:
                    AccountEmail.send(session, session.getProvider(EmailTemplateProvider.class), realm, user, (emailTemplateProvider, attributes) -> {
                        attributes.put("trustedDeviceInfo", trustedDeviceInfo);
                        emailTemplateProvider.send("acmeTrustedDeviceAddedSubject", List.of(RealmUtils.getDisplayName(realm)), "acme-trusted-device-added.ftl", attributes);
                    });
                    break;
                case REMOVE:
                    AccountEmail.send(session, session.getProvider(EmailTemplateProvider.class), realm, user, (emailTemplateProvider, attributes) -> {
                        attributes.put("trustedDeviceInfo", trustedDeviceInfo);
                        emailTemplateProvider.send("acmeTrustedDeviceRemovedSubject", List.of(RealmUtils.getDisplayName(realm)), "acme-trusted-device-removed.ftl", attributes);
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
        try {
            AccountEmail.send(session, session.getProvider(EmailTemplateProvider.class), realm, user, (emailTemplateProvider, attributes) -> {
                attributes.put("userLoginFailure", userLoginFailure);
                emailTemplateProvider.send("acmeAccountBlockedSubject", List.of(RealmUtils.getDisplayName(realm)), "acme-account-blocked.ftl", attributes);
            });
        } catch (EmailException e) {
            log.errorf(e, "Failed to send email for user account block. userId=%s", userLoginFailure.getUserId());
        }
    }

    public static void onAccountUpdate(KeycloakSession session, RealmModel realm, UserModel user, AccountChange update) {
        try {
            AccountEmail.send(session, session.getProvider(EmailTemplateProvider.class), realm, user, (emailTemplateProvider, attributes) -> {
                attributes.put("update", update);
                emailTemplateProvider.send("acmeAccountUpdatedSubject", List.of(RealmUtils.getDisplayName(realm)), "acme-account-updated.ftl", attributes);
            });
        } catch (EmailException e) {
            log.errorf(e, "Failed to send email for user account update. userId=%s", user.getId());
        }
    }

    private static String getCredentialLabel(CredentialModel credential) {

        var type = credential.getType();
        if (OTPCredentialModel.TYPE.equals(type)) {
            return type.toUpperCase();
        }

        var label = credential.getUserLabel();
        if (label == null || label.isEmpty()) {
            return "";
        }

        return credential.getUserLabel();
    }

    public static void onCredentialChange(KeycloakSession session, RealmModel realm, UserModel user, CredentialModel credential, MfaChange change) {
        log.debugf("credential change %s", change);

        if (WebAuthnCredentialModel.TYPE_PASSWORDLESS.equals(credential.getType())) {
            onUserPasskeyChanged(session, realm, user, credential, change);
            return;
        }

        // TODO delegate to onUserMfaChanged
    }

    public static void onUserPasskeyChanged(KeycloakSession session, RealmModel realm, UserModel user, CredentialModel credential, MfaChange change) {

        try {
            var credentialLabel = getCredentialLabel(credential);
            var mfaInfo = new MfaInfo(credential.getType(), credentialLabel);
            switch (change) {
                case ADD:
                    AccountEmail.send(session, session.getProvider(EmailTemplateProvider.class), realm, user, (emailTemplateProvider, attributes) -> {
                        attributes.put("passkeyInfo", mfaInfo);
                        emailTemplateProvider.send("acmePasskeyAddedSubject", List.of(RealmUtils.getDisplayName(realm)), "acme-passkey-added.ftl", attributes);
                    });
                    break;
                case REMOVE:
                    AccountEmail.send(session, session.getProvider(EmailTemplateProvider.class), realm, user, (emailTemplateProvider, attributes) -> {
                        attributes.put("passkeyInfo", mfaInfo);
                        emailTemplateProvider.send("acmePasskeyRemovedSubject", List.of(RealmUtils.getDisplayName(realm)), "acme-passkey-removed.ftl", attributes);
                    });
                    break;
                default:
                    break;
            }
        } catch (EmailException e) {
            log.errorf(e, "Failed to send email for new user passkey change: %s.", change);
        }
    }
}

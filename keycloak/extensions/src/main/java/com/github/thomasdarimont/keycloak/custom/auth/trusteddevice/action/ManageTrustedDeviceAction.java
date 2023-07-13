package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.action;

import com.github.thomasdarimont.keycloak.custom.account.AccountActivity;
import com.github.thomasdarimont.keycloak.custom.account.MfaChange;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.TrustedDeviceCookie;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.TrustedDeviceName;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.TrustedDeviceToken;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials.TrustedDeviceCredentialModel;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials.TrustedDeviceCredentialProvider;
import com.github.thomasdarimont.keycloak.custom.support.RequiredActionUtils;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.sessions.AuthenticationSessionModel;

import jakarta.ws.rs.core.MultivaluedMap;
import java.math.BigInteger;
import java.security.SecureRandom;

@JBossLog
public class ManageTrustedDeviceAction implements RequiredActionProvider {

    public static final String ID = "acme-manage-trusted-device";

    // TODO move to centralized configuration
    public static final int NUMBER_OF_DAYS_TO_TRUST_DEVICE = Integer.getInteger("keycloak.auth.trusteddevice.trustdays", 120);

    private static final boolean HEADLESS_TRUSTED_DEVICE_REGISTRATION_ENABLED = Boolean.parseBoolean(System.getProperty("keycloak.auth.trusteddevice.headless", "true"));

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {
        // NOOP
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {

        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();

        if (HEADLESS_TRUSTED_DEVICE_REGISTRATION_ENABLED) {
            // derive trusted device from ser agent

            KeycloakSession session = context.getSession();

            // automatically generated device name based on Browser and OS.
            String deviceName = TrustedDeviceName.generateDeviceName(context.getHttpRequest());

            registerNewTrustedDevice(session, realm, user, deviceName, null);
            afterTrustedDeviceRegistration(context, new TrustedDeviceInfo(deviceName));
            return;
        }

        String username = user.getUsername();
        String deviceName = TrustedDeviceName.generateDeviceName(context.getHttpRequest());

        LoginFormsProvider form = context.form();
        form.setAttribute("username", username);
        form.setAttribute("device", deviceName);
        context.challenge(form.createForm("manage-trusted-device-form.ftl"));
    }

    @Override
    public void processAction(RequiredActionContext context) {

        if (RequiredActionUtils.isCancelApplicationInitiatedAction(context)) {
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            AuthenticationManager.setKcActionStatus(ManageTrustedDeviceAction.ID, RequiredActionContext.KcActionStatus.CANCELLED, authSession);
            context.success();
            return;
        }

        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();
        HttpRequest httpRequest = context.getHttpRequest();
        MultivaluedMap<String, String> formParams = httpRequest.getDecodedFormParameters();

        // register trusted device

        if (formParams.containsKey("remove-other-trusted-devices")) {
            log.info("Remove all trusted device registrations");
            removeTrustedDevices(context);
        }

        var receivedTrustedDeviceToken = TrustedDeviceCookie.parseDeviceTokenFromCookie(httpRequest, session);

        if (formParams.containsKey("dont-trust-device")) {
            log.info("Remove trusted device registration");

            TrustedDeviceCredentialModel trustedDeviceModel = TrustedDeviceCredentialModel.lookupTrustedDevice(user, receivedTrustedDeviceToken);
            if (trustedDeviceModel != null) {
                boolean deleted = session.getProvider(CredentialProvider.class, TrustedDeviceCredentialProvider.ID).deleteCredential(realm, user, trustedDeviceModel.getId());
                if (deleted) {

                    AccountActivity.onTrustedDeviceChange(session, realm, user, new TrustedDeviceInfo(trustedDeviceModel.getUserLabel()), MfaChange.REMOVE);
                }
            }
        }

        if (formParams.containsKey("trust-device")) {
            String deviceName = TrustedDeviceName.sanitizeDeviceName(formParams.getFirst("device"));
            registerNewTrustedDevice(session, realm, user, deviceName, receivedTrustedDeviceToken);
            afterTrustedDeviceRegistration(context, new TrustedDeviceInfo(deviceName));
        }


        // remove required action if present
        context.getUser().removeRequiredAction(ID);
        context.success();
    }

    private void afterTrustedDeviceRegistration(RequiredActionContext context, TrustedDeviceInfo trustedDeviceInfo) {
        // remove required action if present
        context.getUser().removeRequiredAction(ID);
        context.success();

        EventBuilder event = context.getEvent();
        event.event(EventType.CUSTOM_REQUIRED_ACTION);
        event.detail("action_id", ID);
        event.detail("register_trusted_device", "true");
        event.success();

        AccountActivity.onTrustedDeviceChange(context.getSession(), context.getRealm(), context.getUser(), trustedDeviceInfo, MfaChange.ADD);
    }

    private void registerNewTrustedDevice(KeycloakSession session, RealmModel realm, UserModel user, String deviceName, TrustedDeviceToken receivedTrustedDeviceToken) {

        TrustedDeviceCredentialModel currentTrustedDevice = TrustedDeviceCredentialModel.lookupTrustedDevice(user, receivedTrustedDeviceToken);

        if (currentTrustedDevice == null) {
            log.info("Register new trusted device");
        } else {
            log.info("Update existing trusted device");
        }

        int numberOfDaysToTrustDevice = NUMBER_OF_DAYS_TO_TRUST_DEVICE; //FIXME make name of days to remember deviceToken configurable

        String deviceId = currentTrustedDevice == null ? null : currentTrustedDevice.getDeviceId();
        TrustedDeviceToken newTrustedDeviceToken = createDeviceToken(deviceId, numberOfDaysToTrustDevice);

        if (currentTrustedDevice == null) {
            var tdcm = new TrustedDeviceCredentialModel(null, deviceName, newTrustedDeviceToken.getDeviceId());
            var cp = session.getProvider(CredentialProvider.class, TrustedDeviceCredentialProvider.ID);
            cp.createCredential(realm, user, tdcm);
        } else {
            // update label name for existing device
            user.credentialManager().updateCredentialLabel(currentTrustedDevice.getId(), deviceName);
        }

        String deviceTokenString = session.tokens().encode(newTrustedDeviceToken);
        int maxAge = numberOfDaysToTrustDevice * 24 * 60 * 60;
        TrustedDeviceCookie.addDeviceCookie(deviceTokenString, maxAge, session, realm);
        log.info("Registered trusted device");
    }

    private void removeTrustedDevices(RequiredActionContext context) {

        var user = context.getUser();
        var scm = user.credentialManager();

        scm.getStoredCredentialsByTypeStream(TrustedDeviceCredentialModel.TYPE).forEach(cm -> scm.removeStoredCredentialById(cm.getId()));
    }

    protected TrustedDeviceToken createDeviceToken(String deviceId, int numberOfDaysToTrustDevice) {

        // TODO enhance generated device id with information from httpRequest, e.g. browser fingerprint

        String currentDeviceId = deviceId;
        // generate a unique but short device id
        if (currentDeviceId == null) {
            currentDeviceId = BigInteger.valueOf(new SecureRandom().nextLong()).toString(36);
        }
        TrustedDeviceToken trustedDeviceToken = new TrustedDeviceToken();

        long iat = Time.currentTime();
        long exp = iat + (long) numberOfDaysToTrustDevice * 24 * 60 * 60;
        trustedDeviceToken.iat(iat);
        trustedDeviceToken.exp(exp);
        trustedDeviceToken.setDeviceId(currentDeviceId);
        return trustedDeviceToken;
    }

    @Override
    public void close() {
        // NOOP
    }

    @AutoService(RequiredActionFactory.class)
    public static class Factory implements RequiredActionFactory {

        public static final ManageTrustedDeviceAction INSTANCE = new ManageTrustedDeviceAction();

        @Override
        public RequiredActionProvider create(KeycloakSession session) {
            return INSTANCE;
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
        public void close() {
            // NOOP
        }

        @Override
        public String getId() {
            return ManageTrustedDeviceAction.ID;
        }

        @Override
        public String getDisplayText() {
            return "Acme: Manage Trusted Device";
        }

        @Override
        public boolean isOneTimeAction() {
            return true;
        }
    }
}
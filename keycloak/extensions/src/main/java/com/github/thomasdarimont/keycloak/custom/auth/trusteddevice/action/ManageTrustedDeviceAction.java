package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.action;

import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.TrustedDeviceCookie;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.TrustedDeviceName;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.TrustedDeviceToken;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.auth.TrustedDeviceAuthenticator;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials.TrustedDeviceCredentialModel;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials.TrustedDeviceCredentialProviderFactory;
import com.github.thomasdarimont.keycloak.custom.support.RequiredActionUtils;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.math.BigInteger;
import java.security.SecureRandom;

@JBossLog
public class ManageTrustedDeviceAction implements RequiredActionProvider {

    public static final String ID = "acme-manage-trusted-device";

    private static final int NUMBER_OF_DAYS_TO_TRUST_DEVICE = Integer.getInteger("keycloak.acme.auth.trusteddevice.trustdays", 120);

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
        context.challenge(generateRegisterTrustedDeviceForm(context));
    }

    protected Response generateRegisterTrustedDeviceForm(RequiredActionContext context) {
        return createRegisterTrustedDeviceForm(context).createForm("manage-trusted-device-form.ftl");
    }

    protected LoginFormsProvider createRegisterTrustedDeviceForm(RequiredActionContext context) {

        UserModel user = context.getAuthenticationSession().getAuthenticatedUser();
        String username = user.getUsername();
        String deviceName = TrustedDeviceName.generateDeviceName(context.getHttpRequest());

        LoginFormsProvider form = context.form();
        form.setAttribute("username", username);
        form.setAttribute("device", deviceName);
        return form;
    }

    @Override
    public void processAction(RequiredActionContext context) {

        if (RequiredActionUtils.isCancelApplicationInitiatedAction(context)) {
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            AuthenticationManager.setKcActionStatus(ManageTrustedDeviceAction.ID, RequiredActionContext.KcActionStatus.CANCELLED, authSession);
            context.success();
            return;
        }

        EventBuilder event = context.getEvent();
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

        if (formParams.containsKey("dont-trust-device")) {
            log.info("Remove trusted device registration");

            TrustedDeviceCredentialModel trustedDeviceModel = TrustedDeviceAuthenticator.lookupTrustedDeviceCredentialModelFromCookie(session, realm, user, httpRequest);
            if (trustedDeviceModel != null) {
                session.getProvider(CredentialProvider.class, TrustedDeviceCredentialProviderFactory.ID)
                        .deleteCredential(realm, user, trustedDeviceModel.getId());
            }
        }

        if (formParams.containsKey("trust-device")) {
            TrustedDeviceCredentialModel currentTrustedDevice = TrustedDeviceAuthenticator.lookupTrustedDeviceCredentialModelFromCookie(session, realm, user, httpRequest);

            if (currentTrustedDevice == null) {
                log.info("Register new trusted device");
            } else {
                log.info("Update existing trusted device");
            }

            int numberOfDaysToTrustDevice = NUMBER_OF_DAYS_TO_TRUST_DEVICE; //FIXME make name of days to remember deviceToken configurable

            String deviceId = currentTrustedDevice == null ? null : currentTrustedDevice.getDeviceId();
            TrustedDeviceToken trustedDeviceToken = createDeviceToken(deviceId, numberOfDaysToTrustDevice);
            String deviceName = TrustedDeviceName.sanitizeDeviceName(formParams.getFirst("device"));

            if (currentTrustedDevice == null) {
                TrustedDeviceCredentialModel tdcm = new TrustedDeviceCredentialModel(null, deviceName, trustedDeviceToken.getDeviceId());
                session.userCredentialManager().createCredentialThroughProvider(realm, user, tdcm);
            } else {
                // update label name for existing device
                session.userCredentialManager().updateCredentialLabel(realm, user, currentTrustedDevice.getId(), deviceName);
            }

            String deviceTokenString = session.tokens().encode(trustedDeviceToken);
            int maxAge = numberOfDaysToTrustDevice * 24 * 60 * 60;
            TrustedDeviceCookie.addDeviceCookie(deviceTokenString, maxAge, session, realm);
            log.info("Registered trusted device");
        }

        // remove required action if present
        context.getUser().removeRequiredAction(ID);
        context.success();

        event.event(EventType.CUSTOM_REQUIRED_ACTION);
        event.detail("action_id", ID);
        event.detail("register_trusted_device", "true");
        event.success();
    }

    private void removeTrustedDevices(RequiredActionContext context) {

        KeycloakSession session = context.getSession();
        UserCredentialManager ucm = session.userCredentialManager();

        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();

        ucm.getStoredCredentialsByTypeStream(realm, user, TrustedDeviceCredentialModel.TYPE)
                .forEach(cm -> ucm.removeStoredCredential(realm, user, cm.getId()));
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
}

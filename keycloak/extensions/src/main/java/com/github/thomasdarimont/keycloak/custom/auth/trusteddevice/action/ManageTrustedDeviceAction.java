package com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.action;

import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.DeviceCookie;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.DeviceToken;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.credentials.TrustedDeviceCredentialModel;
import com.github.thomasdarimont.keycloak.custom.auth.trusteddevice.support.UserAgentParser;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.Time;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import ua_parser.OS;
import ua_parser.UserAgent;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.math.BigInteger;
import java.security.SecureRandom;

@JBossLog
public class ManageTrustedDeviceAction implements RequiredActionProvider {

    public static final String ID = "acme-manage-trusted-device";

    private static final PolicyFactory TEXT_ONLY_SANITIZATION_POLICY = new HtmlPolicyBuilder().toFactory();

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
        String deviceName = generateDeviceName(context);

        LoginFormsProvider form = context.form();
        form.setAttribute("username", username);
        form.setAttribute("device", deviceName);
        return form;
    }

    private String generateDeviceName(RequiredActionContext context) {
        HttpRequest request = context.getHttpRequest();

        String userAgentString = request.getHttpHeaders().getHeaderString(HttpHeaders.USER_AGENT);
        String name = "Browser";

        // TODO generate a better device name based on the user agent
        UserAgent userAgent = UserAgentParser.parseUserAgent(userAgentString);
        if (userAgent == null) {
            return name;
        }

        OS os = UserAgentParser.parseOperationSystem(userAgentString);
        String osName = "";
        if (os != null) {
            osName = "(" + os.family + ")";
        }

        return name + " " + userAgent.family + " " + osName;
    }


    @Override
    public void processAction(RequiredActionContext context) {

        if (isCancelApplicationInitiatedAction(context)) {
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
            log.info("Skip trusted device registration");
            DeviceCookie.removeDeviceCookie(session, realm);
        }

        if (formParams.containsKey("trust-device")) {
            log.info("Register trusted device");

            int numberOfDaysToTrustDevice = 120; //FIXME make name of days to remember deviceToken configurable

            DeviceToken deviceToken = createDeviceToken(httpRequest, numberOfDaysToTrustDevice);

            String deviceName = sanitizeDeviceName(formParams.getFirst("device"));

            TrustedDeviceCredentialModel tdcm = new TrustedDeviceCredentialModel(deviceName, deviceToken.getDeviceId());
            session.userCredentialManager().createCredentialThroughProvider(realm, user, tdcm);

            String deviceTokenString = session.tokens().encode(deviceToken);
            int maxAge = numberOfDaysToTrustDevice * 24 * 60 * 60;
            DeviceCookie.addDeviceCookie(deviceTokenString, maxAge, session, realm);
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

    private String sanitizeDeviceName(String deviceNameInput) {

        String deviceName = deviceNameInput;

        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = "Browser";
        } else if (deviceName.length() > 32) {
            deviceName = deviceName.substring(0, 32);
        }

        deviceName = TEXT_ONLY_SANITIZATION_POLICY.sanitize(deviceName);
        deviceName = deviceName.trim();

        return deviceName;
    }

    protected DeviceToken createDeviceToken(HttpRequest httpRequest, int numberOfDaysToTrustDevice) {

        // TODO enhance generated device id with information from httpRequest, e.g. browser fingerprint

        // generate a unique but short device id
        String deviceId = BigInteger.valueOf(new SecureRandom().nextLong()).toString(36);
        DeviceToken deviceToken = new DeviceToken();

        long iat = Time.currentTime();
        long exp = iat + (long)numberOfDaysToTrustDevice * 24 * 60 * 60;
        deviceToken.iat(iat);
        deviceToken.exp(exp);
        deviceToken.setDeviceId(deviceId);
        return deviceToken;
    }

    protected boolean isCancelApplicationInitiatedAction(RequiredActionContext context) {

        HttpRequest httpRequest = context.getHttpRequest();
        MultivaluedMap<String, String> formParams = httpRequest.getDecodedFormParameters();
        return formParams.containsKey(LoginActionsService.CANCEL_AIA);
    }

    @Override
    public void close() {
        // NOOP
    }
}

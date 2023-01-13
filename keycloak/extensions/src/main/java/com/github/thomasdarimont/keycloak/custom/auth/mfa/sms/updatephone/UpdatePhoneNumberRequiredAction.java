package com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.updatephone;

import com.github.thomasdarimont.keycloak.custom.account.AccountActivity;
import com.github.thomasdarimont.keycloak.custom.account.MfaChange;
import com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.PhoneNumberUtils;
import com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.SmsAuthenticator;
import com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.SmsCodeSender;
import com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.client.SmsClientFactory;
import com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.credentials.SmsCredentialModel;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.Time;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@JBossLog
public class UpdatePhoneNumberRequiredAction implements RequiredActionProvider {

    public static final String ID = "acme-update-phonenumber";

    private static final String PHONE_NUMBER_FIELD = "mobile";

    private static final String PHONE_NUMBER_ATTRIBUTE = "phoneNumber";
    private static final String PHONE_NUMBER_VERIFIED_ATTRIBUTE = "phoneNumberVerified";

    private static final String PHONE_NUMBER_AUTH_NOTE = ID + "-number";
    private static final String FORM_ACTION_UPDATE = "update";
    private static final String FORM_ACTION_VERIFY = "verify";

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        // whether we can refer to that action via kc_actions URL parameter
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

        // check whether we need to show the update custom info form.

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        if (!ID.equals(authSession.getClientNotes().get(Constants.KC_ACTION))) {
            // only show update form if we explicitly asked for the required action execution
            return;
        }

        UserModel user = context.getUser();
        if (user.getFirstAttribute(PHONE_NUMBER_ATTRIBUTE) == null) {
            user.addRequiredAction(ID);
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {

        // Show form
        context.challenge(createForm(context, null));
    }

    protected Response createForm(RequiredActionContext context, Consumer<LoginFormsProvider> formCustomizer) {

        LoginFormsProvider form = context.form();
        UserModel user = context.getUser();
        form.setAttribute("username", user.getUsername());

        String phoneNumber = user.getFirstAttribute(PHONE_NUMBER_ATTRIBUTE);
        form.setAttribute("currentMobile", phoneNumber == null ? "" : phoneNumber);

        if (formCustomizer != null) {
            formCustomizer.accept(form);
        }

        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        if (authSession.getAuthNote(PHONE_NUMBER_AUTH_NOTE) != null) {
            // we are already sent a code
            return form.createForm("update-phone-number-form.ftl");
        }

        // use form from src/main/resources/theme-resources/templates/
        return form.createForm("update-phone-number-form.ftl");
    }

    @Override
    public void processAction(RequiredActionContext context) {

        // TODO trigger phone number verification via SMS
        // user submitted the form
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        EventBuilder event = context.getEvent();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        RealmModel realm = context.getRealm();
        UserModel user = context.getUser();
        KeycloakSession session = context.getSession();

        event.event(EventType.UPDATE_PROFILE);
        String phoneNumber = formData.getFirst(PHONE_NUMBER_FIELD);

        EventBuilder errorEvent = event.clone().event(EventType.UPDATE_PROFILE_ERROR).client(authSession.getClient()).user(authSession.getAuthenticatedUser());

        if (formData.getFirst(FORM_ACTION_UPDATE) != null) {

            if (!isValidPhoneNumber(phoneNumber)) {

                Response challenge = createForm(context, form -> {
                    form.addError(new FormMessage(PHONE_NUMBER_FIELD, "Invalid Input"));
                });
                context.challenge(challenge);
                errorEvent.error(Errors.INVALID_INPUT);
                return;
            }

            LoginFormsProvider form = context.form();
            form.setAttribute("currentMobile", phoneNumber);

            boolean useWebOtp = true;
            boolean result = createSmsSender(context).sendVerificationCode(session, realm, user, phoneNumber, Map.of("client", SmsClientFactory.MOCK_SMS_CLIENT), SmsAuthenticator.VERIFY_CODE_LENGTH, SmsAuthenticator.CODE_TTL, useWebOtp, authSession);
            if (!result) {
                log.warnf("Failed to send sms message. realm=%s user=%s", realm.getName(), user.getId());
            }

            authSession.setAuthNote(PHONE_NUMBER_AUTH_NOTE, phoneNumber);
            form.setInfo("smsSentInfo", phoneNumber);
            context.challenge(form.createForm("verify-phone-number-form.ftl"));
            return;
        }

        if (formData.getFirst(FORM_ACTION_VERIFY) != null) {
            String phoneNumberFromAuthNote = authSession.getAuthNote(PHONE_NUMBER_AUTH_NOTE);
            String expectedCode = context.getAuthenticationSession().getAuthNote(SmsAuthenticator.AUTH_NOTE_CODE);

            // TODO check max failed attempts

            String actualCode = formData.getFirst("code");
            if (!expectedCode.equals(actualCode)) {
                LoginFormsProvider form = context.form();
                form.setAttribute("currentMobile", phoneNumberFromAuthNote);
                form.setErrors(List.of(new FormMessage("code", "error-invalid-code")));
                context.challenge(form.createForm("verify-phone-number-form.ftl"));
                return;
            }

            user.setSingleAttribute(PHONE_NUMBER_ATTRIBUTE, phoneNumberFromAuthNote);
            user.setSingleAttribute(PHONE_NUMBER_VERIFIED_ATTRIBUTE, "true");
            user.removeRequiredAction(ID);

            afterPhoneNumberVerified(realm, user, session, phoneNumberFromAuthNote);

            context.success();
            return;
        }

        context.failure();
    }

    protected void afterPhoneNumberVerified(RealmModel realm, UserModel user, KeycloakSession session, String phoneNumberFromAuthNote) {
        // TODO split this up into a separate required action, e.g. UpdateMfaSmsCodeRequiredAction
        updateSmsMfaCredential(realm, user, session, phoneNumberFromAuthNote);
    }

    protected void updateSmsMfaCredential(RealmModel realm, UserModel user, KeycloakSession session, String phoneNumber) {

        var credentialManager = user.credentialManager();
        credentialManager.getStoredCredentialsByTypeStream(SmsCredentialModel.TYPE).forEach(cm -> credentialManager.removeStoredCredentialById(cm.getId()));

        SmsCredentialModel model = new SmsCredentialModel();
        model.setPhoneNumber(phoneNumber);
        model.setType(SmsCredentialModel.TYPE);
        model.setCreatedDate(Time.currentTimeMillis());
        model.setUserLabel("SMS @ " + PhoneNumberUtils.abbreviatePhoneNumber(phoneNumber));
        model.writeCredentialData();

        var credential = user.credentialManager().createStoredCredential(model);
        if (credential != null) {
            AccountActivity.onUserMfaChanged(session, realm, user, credential, MfaChange.ADD);
        }
    }

    private static boolean isValidPhoneNumber(String phoneNumber) {

        if (phoneNumber == null) {
            return false;
        }

        String phone = phoneNumber.trim();
        // TODO use libphonenumber to validate phone number here
        return phone.length() > 3;
    }

    protected SmsCodeSender createSmsSender(RequiredActionContext context) {
        return new SmsCodeSender();
    }

    @Override
    public void close() {
        // NOOP
    }

    @AutoService(RequiredActionFactory.class)
    public static class Factory implements RequiredActionFactory {

        private static final RequiredActionProvider INSTANCE = new UpdatePhoneNumberRequiredAction();

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
            return UpdatePhoneNumberRequiredAction.ID;
        }

        @Override
        public String getDisplayText() {
            return "Acme: Update Mobile Phonenumber";
        }

        @Override
        public boolean isOneTimeAction() {
            return true;
        }
    }
}

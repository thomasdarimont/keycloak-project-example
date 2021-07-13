package com.github.thomasdarimont.keycloak.custom.mfa.sms.updatephone;

import com.github.thomasdarimont.keycloak.custom.mfa.sms.SmsAuthenticator;
import com.github.thomasdarimont.keycloak.custom.mfa.sms.SmsCodeSender;
import com.github.thomasdarimont.keycloak.custom.mfa.sms.client.SmsClientFactory;
import com.github.thomasdarimont.keycloak.custom.mfa.sms.credentials.SmsCredentialModel;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@JBossLog
public class UpdatePhoneNumberRequiredAction implements RequiredActionProvider {

    public static final String ID = "acme-update-phonenumber";

    public static final String PHONE_NUMBER_FIELD = "mobile";

    public static final String PHONE_NUMBER_ATTRIBUTE = "phone_number";
    public static final String PHONE_NUMBER_VERIFIED_ATTRIBUTE = "phone_number_verified";

    public static final int VERIFY_CODE_LENGTH = 6;

    private static final String PHONENUMBER_AUTH_NOTE = ID + "-number";

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        // whether we can refer to that action via kc_actions URL parameter
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

        // check whether we need to show the update custom info form.

        if (!ID.equals(context.getAuthenticationSession().getClientNotes().get("kc_action"))) {
            // only show update form if we explicitly asked for the required action execution
            return;
        }

        if (context.getUser().getFirstAttribute(PHONE_NUMBER_ATTRIBUTE) == null) {
            context.getUser().addRequiredAction(ID);
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {

        // Show form
        context.challenge(createForm(context, null));
    }

    protected Response createForm(RequiredActionContext context, Consumer<LoginFormsProvider> formCustomizer) {

        LoginFormsProvider form = context.form();
        form.setAttribute("username", context.getUser().getUsername());

        String phoneNumber = context.getUser().getFirstAttribute(PHONE_NUMBER_ATTRIBUTE);
        form.setAttribute("currentMobile", phoneNumber == null ? "" : phoneNumber);

        if (formCustomizer != null) {
            formCustomizer.accept(form);
        }

        // use form from src/main/resources/theme-resources/templates/
        return form.createForm("update-phone-number-form.ftl");
    }

    @Override
    public void processAction(RequiredActionContext context) {

        if (isCancelApplicationInitiatedAction(context)) {
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            AuthenticationManager.setKcActionStatus(UpdatePhoneNumberRequiredAction.ID, RequiredActionContext.KcActionStatus.CANCELLED, authSession);
            authSession.removeAuthNote(SmsAuthenticator.AUTH_NOTE_CODE);
            authSession.removeAuthNote(PHONENUMBER_AUTH_NOTE);
            context.success();
            return;
        }

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

        EventBuilder errorEvent = event.clone().event(EventType.UPDATE_PROFILE_ERROR)
                .client(authSession.getClient())
                .user(authSession.getAuthenticatedUser());


        if (formData.getFirst("update") != null) {

            if (Validation.isBlank(phoneNumber) || phoneNumber.length() < 3) {

                Response challenge = createForm(context, form -> {
                    form.addError(new FormMessage(PHONE_NUMBER_FIELD, "Invalid Input"));
                });

                context.challenge(challenge);

                errorEvent.error(Errors.INVALID_INPUT);
                return;
            }


            LoginFormsProvider form = context.form();
            form.setAttribute("currentMobile", phoneNumber);

            boolean result = new SmsCodeSender().sendVerificationCode(session, realm, user, phoneNumber, Map.of("client", SmsClientFactory.MOCK_SMS_CLIENT), VERIFY_CODE_LENGTH, 300,
                    authSession);

            authSession.setAuthNote(PHONENUMBER_AUTH_NOTE, phoneNumber);
            form.setInfo("smsSentInfo", phoneNumber);
            context.challenge(form.createForm("verify-phone-number-form.ftl"));
            return;
        }

        if (formData.getFirst("verify") != null) {
            String phoneNumberFromAuthNote = authSession.getAuthNote(PHONENUMBER_AUTH_NOTE);
            String expectedCode = context.getAuthenticationSession().getAuthNote(SmsAuthenticator.AUTH_NOTE_CODE);
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

            updateSmsMfaCredential(realm, user, session, phoneNumberFromAuthNote);

            context.success();
            return;
        }

        context.failure();
    }

    private void updateSmsMfaCredential(RealmModel realm, UserModel user, KeycloakSession session, String phoneNumber) {

        UserCredentialManager ucm = session.userCredentialManager();
        ucm.getStoredCredentialsByTypeStream(realm, user, SmsCredentialModel.TYPE).forEach(
                cm -> ucm.removeStoredCredential(realm, user, cm.getId())
        );

        SmsCredentialModel model = new SmsCredentialModel();
        model.setPhoneNumber(phoneNumber);

        ucm.createCredentialThroughProvider(realm, user, model);
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

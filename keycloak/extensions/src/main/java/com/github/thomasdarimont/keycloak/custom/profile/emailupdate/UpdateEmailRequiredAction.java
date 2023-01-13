package com.github.thomasdarimont.keycloak.custom.profile.emailupdate;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.Config;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.beans.ProfileBean;
import org.keycloak.events.Details;
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
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.validation.Validation;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.storage.adapter.InMemoryUserAdapter;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

@JBossLog
public class UpdateEmailRequiredAction implements RequiredActionProvider {

    public static final String ID = "acme-update-email";

    public static final String AUTH_NOTE_CODE = "emailCode";

    public static final String EMAIL_FIELD = "email";

    public static final int VERIFY_CODE_LENGTH = 6;

    private static final String UPDATE_EMAIL_AUTH_NOTE = ID;

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

        if (context.getUser().getEmail() == null) {
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

        if (context.getAuthenticationSession().getAuthNote(UPDATE_EMAIL_AUTH_NOTE) != null) {
            // we are already sent a code
            return form.createForm("verify-email-form.ftl");
        }

        String email = context.getUser().getEmail();
        form.setAttribute("currentEmail", email);

        if (formCustomizer != null) {
            formCustomizer.accept(form);
        }

        // use form from src/main/resources/theme-resources/templates/
        return form.createForm("update-email-form.ftl");
    }

    @Override
    public void processAction(RequiredActionContext context) {

        // TODO trigger email verification via email
        // user submitted the form
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        EventBuilder event = context.getEvent().clone().event(EventType.UPDATE_EMAIL);
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        RealmModel realm = context.getRealm();
        UserModel currentUser = context.getUser();
        KeycloakSession session = context.getSession();

        String newEmail = String.valueOf(formData.getFirst(EMAIL_FIELD)).trim();

        event.detail(Details.EMAIL, newEmail);

        EventBuilder errorEvent = event.clone().event(EventType.UPDATE_EMAIL_ERROR)
                .client(authSession.getClient())
                .user(authSession.getAuthenticatedUser());


        if (formData.getFirst("update") != null) {
            final String emailError;
            if (Validation.isBlank(newEmail) || !Validation.isEmailValid(newEmail)) {
                emailError = "invalidEmailMessage";
                errorEvent.detail("error", "invalid-email-format");
            } else if (Objects.equals(newEmail, currentUser.getEmail())) {
                emailError = "invalidEmailSameAddressMessage";
                errorEvent.detail("error", "invalid-email-same-email");
            } else if (session.users().getUserByEmail(realm, newEmail) != null) {
                emailError = "invalidEmailMessage";
                errorEvent.detail("error", "invalid-email-already-in-use");
            } else {
                emailError = null;
            }

            if (emailError != null) {
                errorEvent.error(Errors.INVALID_INPUT);

                Response challenge = createForm(context, form -> {
                    form.addError(new FormMessage(EMAIL_FIELD, emailError));
                });

                context.challenge(challenge);
                return;
            }

            String code = SecretGenerator.getInstance().randomString(VERIFY_CODE_LENGTH).toLowerCase();
            authSession.setAuthNote(AUTH_NOTE_CODE, code);

            LoginFormsProvider form = context.form();
            form.setAttribute("currentEmail", newEmail);

            try {
                EmailTemplateProvider emailTemplateProvider = session.getProvider(EmailTemplateProvider.class);
                emailTemplateProvider.setRealm(realm);

                // adapt current user to be able to override the email for the verification email
                UserModel userAdapter = new InMemoryUserAdapter(session, realm, currentUser.getId());
                userAdapter.setEmail(newEmail);
                userAdapter.setUsername(currentUser.getUsername());
                userAdapter.setFirstName(currentUser.getFirstName());
                userAdapter.setLastName(currentUser.getLastName());

                emailTemplateProvider.setUser(userAdapter);

                Map<String, Object> attributes = new HashMap<>();
                attributes.put("code", code);
                attributes.put("user", new ProfileBean(currentUser) {
                    @Override
                    public String getEmail() {
                        return newEmail;
                    }
                });

                String realmDisplayName = realm.getDisplayName();
                if (realmDisplayName == null) {
                    realmDisplayName = realm.getName();
                }
                emailTemplateProvider.send("acmeEmailVerifySubject", List.of(realmDisplayName),
                        "acme-email-verification-with-code.ftl", attributes);

                authSession.setAuthNote(UPDATE_EMAIL_AUTH_NOTE, newEmail);
                form.setInfo("emailSentInfo", newEmail);
                context.challenge(form.createForm("verify-email-form.ftl"));

            } catch (EmailException e) {
                log.errorf(e, "Could not send verify email.");
                context.failure();
            }
            return;
        }

        if (formData.getFirst("verify") != null) {
            String emailFromAuthNote = authSession.getAuthNote(UPDATE_EMAIL_AUTH_NOTE);
            String expectedCode = authSession.getAuthNote(AUTH_NOTE_CODE);
            String actualCode = String.valueOf(formData.getFirst("code")).trim();
            if (!expectedCode.equals(actualCode)) {
                LoginFormsProvider form = context.form();
                form.setAttribute("currentEmail", emailFromAuthNote);
                form.setErrors(List.of(new FormMessage("code", "error-invalid-code")));
                context.challenge(form.createForm("verify-email-form.ftl"));
                return;
            }

            currentUser.setEmail(emailFromAuthNote);
            currentUser.setEmailVerified(true);
            currentUser.removeRequiredAction(ID);

            event.success();

            context.success();
            return;
        }

        context.failure();
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


    @AutoService(RequiredActionFactory.class)
    public static class Factory implements RequiredActionFactory {

        private static final UpdateEmailRequiredAction INSTANCE = new UpdateEmailRequiredAction();

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
            return UpdateEmailRequiredAction.ID;
        }

        @Override
        public String getDisplayText() {
            return "Acme: Update Email";
        }

        @Override
        public boolean isOneTimeAction() {
            return true;
        }
    }

}

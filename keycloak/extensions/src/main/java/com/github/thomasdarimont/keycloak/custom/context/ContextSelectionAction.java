package com.github.thomasdarimont.keycloak.custom.context;

import com.github.thomasdarimont.keycloak.custom.support.UserSessionUtils;
import com.google.auto.service.AutoService;
import lombok.Data;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.List;
import java.util.function.Consumer;

/**
 * Example for prompting a user for a context selection after authentication.
 * The context selection key will then be stored in the user session and can be exposed to clients.
 * Context selection key could be the key of a business entity (tenant, project, etc.).
 * <p>
 */
@JBossLog
public class ContextSelectionAction implements RequiredActionProvider {

    public static final String ID = "acme-context-selection-action";

    private static final String CONTEXT_KEY = "acme.context.key";

    private static final String CONTEXT_SELECTION_PARAM = "contextSelectionKey";

    private static final String CONTEXT_FORM_ATTRIBUTE = "context.selection.key";


    /**
     * Allows explicit usage via auth url parameter kc_action=acme-context-selection-action
     *
     * @return
     */
    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public int getMaxAuthAge() {
        // don't require reauth to switch contexts within a day
        return 60 * 60 * 24;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

        var authSession = context.getAuthenticationSession();
        // prevents repeated required action execution within the same authentication session
        if (authSession.getAuthNote(ID) != null) {
            return;
        }
        authSession.setAuthNote(ID, "true"); // mark this action as applied

        // TODO check if context selection is required

        // TODO allow to accept contextSelectionKey via URL Parameter

        // handle dynamic context selection for legacy apps with grant_type=password
        var formParams = context.getHttpRequest().getFormParameters();
        if (OAuth2Constants.PASSWORD.equals(formParams.getFirst(OAuth2Constants.GRANT_TYPE))) {
            // allow to accept contextSelectionKey via form post Parameter
            if (formParams.containsKey(CONTEXT_SELECTION_PARAM)) {
                var contextKey = formParams.getFirst(CONTEXT_SELECTION_PARAM);
                if (isValidContextKey(context, contextKey)) {
                    authSession.setUserSessionNote(CONTEXT_KEY, contextKey);
                } else {
                    // contextSelectionKey provided with invalid value
                    context.failure();
                }
            }
            authSession.removeRequiredAction(ID);
            return;
        }

        // handle dynamic context selection for standard flow

        // check if context selection already happened in another user session?
        var userSession = UserSessionUtils.getUserSessionFromAuthenticationSession(context.getSession(), context.getAuthenticationSession());

        // Note, if the user just authenticated there is no user session yet.
        if (userSession != null) {
            var userSessionNotes = userSession.getNotes();
            if (userSessionNotes.containsKey(CONTEXT_KEY)) {
                authSession.removeRequiredAction(ID);
                return;
            }
        }

        // add this required action to the auth session to force execution after authentication
        authSession.addRequiredAction(ID);
    }

    private boolean isValidContextKey(RequiredActionContext context, String contextKey) {
        var options = computeContextOptions(context);
        var foundValidContextKey = false;
        for (var option : options) {
            if (option.getKey().equals(contextKey)) {
                foundValidContextKey = true;
                break;
            }
        }
        return foundValidContextKey;
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        showContextSelectionForm(context, null);
    }

    private void showContextSelectionForm(RequiredActionContext context, Consumer<LoginFormsProvider> formCustomizer) {

        var allowedContextOptions = computeContextOptions(context);
        // TODO handle case when options are empty

        var currentContextItem = getCurrentContextItem(context, allowedContextOptions);

        // show context selection form
        var form = context.form() //
                .setAttribute("username", context.getUser().getUsername())  //
                .setAttribute("currentContext", currentContextItem) //
                .setAttribute("contextOptions", allowedContextOptions);

        // allow to customize form, e.g. to add custom error messages
        if (formCustomizer != null) {
            formCustomizer.accept(form);
        }

        // Note, see template in internal-modern theme
        var response = form.createForm("context-selection.ftl");
        context.challenge(response);
    }

    private static ContextItem getCurrentContextItem(RequiredActionContext context, List<ContextItem> allowedContextOptions) {

        var userSession = UserSessionUtils.getUserSessionFromAuthenticationSession(context.getSession(), context.getAuthenticationSession());
        var currentContextKey = userSession != null ? userSession.getNote(CONTEXT_KEY) : null;

        if (currentContextKey == null) {
            return null;
        }

        return allowedContextOptions.stream().filter(item -> item.getKey().equals(currentContextKey)).findAny().orElse(null);
    }

    private List<ContextItem> computeContextOptions(RequiredActionContext actionContext) {

        // note, here one would call custom logic to populate the eligible context options

        return List.of( //
                new ContextItem("key1", "Context 1"), //
                new ContextItem("key2", "Context 2"), //
                new ContextItem("key3", "Context 3") //
        );
    }

    @Override
    public void initiatedActionCanceled(KeycloakSession session, AuthenticationSessionModel authSession) {
        // TODO clarify if context selection can be cancelled
        // NOOP
    }

    @Override
    public void processAction(RequiredActionContext context) {

        var formData = context.getHttpRequest().getDecodedFormParameters();

        if (formData.containsKey("cancel")) {
            context.success();
            return;
        }

        if (!formData.containsKey(CONTEXT_FORM_ATTRIBUTE)) {
            // TODO show empty selection is not allowed error
            showContextSelectionForm(context, null);
            return;
        }

        var selectedContextKey = formData.getFirst(CONTEXT_FORM_ATTRIBUTE);

        // check if selected context key is allowed
        var allowedContextOptions = computeContextOptions(context);
        if (allowedContextOptions.stream().filter(item -> item.getKey().equals(selectedContextKey)).findAny().isEmpty()) {
            // TODO show value is not allowed error
            showContextSelectionForm(context, null);
            return;
        }

        // propagate selected context to user session
        log.infof("Switching user context. realm=%s userId=%s contextKey=%s", //
                context.getRealm().getName(), context.getUser().getId(), selectedContextKey);

        context.getAuthenticationSession().setUserSessionNote(CONTEXT_KEY, selectedContextKey);
        context.success();
    }

    @Override
    public void close() {
        // NOOP
    }

    @Data
    public static class ContextItem {

        private final String key;
        private final String label;
    }

    @AutoService(RequiredActionFactory.class)
    public static class Factory implements RequiredActionFactory {

        private static final ContextSelectionAction INSTANCE = new ContextSelectionAction();

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public String getDisplayText() {
            return "Acme: User Context Selection";
        }

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
    }
}

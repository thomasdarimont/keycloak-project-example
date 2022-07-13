package com.github.thomasdarimont.keycloak.custom.consent;

import com.google.auto.service.AutoService;
import lombok.Builder;
import lombok.Data;
import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.authentication.DisplayTypeRequiredActionFactory;
import org.keycloak.authentication.InitiatedActionSupport;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.IDToken;
import org.keycloak.services.util.ResolveRelative;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@AutoService(RequiredActionFactory.class)
public class ConsentSelectionAction implements RequiredActionProvider, RequiredActionFactory, DisplayTypeRequiredActionFactory {

    private static final boolean REQUIRE_UPDATE_PROFILE_AFTER_CONSENT_UPDATE = false;

    private static final String AUTH_SESSION_CONSENT_CHECK_MARKER = "checked";

    private static final Map<String, List<ScopeField>> SCOPE_FIELD_MAPPING;

    static {
        var map = new HashMap<String, List<ScopeField>>();

        map.put(OAuth2Constants.SCOPE_PHONE, List.of(new ScopeField("phoneNumber", "tel", u -> u.getFirstAttribute("phoneNumber")))); //
        map.put(OAuth2Constants.SCOPE_EMAIL, List.of(new ScopeField(IDToken.EMAIL, "email", UserModel::getEmail))); //
        // TODO add dedicated client scope of name
        map.put("name", List.of( //
                new ScopeField(IDToken.GIVEN_NAME, "text", UserModel::getFirstName), //
                new ScopeField(IDToken.FAMILY_NAME, "text", UserModel::getLastName) //
        ));

        SCOPE_FIELD_MAPPING = Collections.unmodifiableMap(map);
    }

    @Override
    public String getId() {
        return "acme-dynamic-consent";
    }

    @Override
    public String getDisplayText() {
        return "Acme: Dynamic Consent selection";
    }

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public RequiredActionProvider createDisplay(KeycloakSession session, String displayType) {
        return create(session);
    }

    @Override
    public InitiatedActionSupport initiatedActionSupport() {
        // whether we can refer to that action via kc_actions URL parameter
        return InitiatedActionSupport.SUPPORTED;
    }

    @Override
    public void evaluateTriggers(RequiredActionContext context) {

        // Decide which use-case is required:
        // Onboard a client with user decisions regarding consent and maybe missing profile attribute collection
        // 1) Client is used for the first time OR
        // 2) Client has changed required scopes OR
        // 3) Client enforced consent screen
        // Decision local data only
        // Assumption: Scope to Profile Attribute mapping is fix (E.g. scope name to attribute first-, lastname cannot have a salutation added later
        //         OR: Remove all consent for name and let the process begin again
        //         OR: A remote call per login/auth-flow

        var authSession = context.getAuthenticationSession();
        var user = context.getUser();

        // For Keycloak versions up to 18.0.2 evaluateTriggers is called multiple times,
        // since we need to perform this check only once per auth session, we use a marker
        // to remember whether the check already took place.
        if (AUTH_SESSION_CONSENT_CHECK_MARKER.equals(authSession.getClientNote(getId()))) {
            return;
        }

        if ("consent-webapp-springboot".equals(authSession.getClient().getClientId())) {
            return;
        }

        var missingConsents = getScopeInfo(context.getSession(), authSession, user);

        var prompt = context.getUriInfo().getQueryParameters().getFirst(OAuth2Constants.PROMPT);
        var explicitConsentRequested = OIDCLoginProtocol.PROMPT_VALUE_CONSENT.equals(prompt);

<<<<<<< HEAD
        if ( // !missingConsents.getMissingRequired().isEmpty() ||
                explicitConsentRequested) {
=======
        var consentMissingForRequiredScopes = !missingConsents.getMissingRequired().isEmpty();
        if (consentMissingForRequiredScopes || explicitConsentRequested) {
>>>>>>> afda0af (keycloak: refactor dynamic consent handling)
            authSession.addRequiredAction(getId());
            authSession.setClientNote(getId(), AUTH_SESSION_CONSENT_CHECK_MARKER);

            if (consentMissingForRequiredScopes && REQUIRE_UPDATE_PROFILE_AFTER_CONSENT_UPDATE) {
                authSession.addRequiredAction(UserModel.RequiredAction.UPDATE_PROFILE);
            }
        } else {
            authSession.removeRequiredAction(getId());
        }
    }

    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        // Show form for user sees scopes (required, optional)
        // AND/OR associated profile attributes
        // AND/OR give user the chance to update/add profile attributes

        // Implementation
        // 1) Local (background sync)
        // 2) Local with remote call
        // 2) Redirect browser

<<<<<<< HEAD
        // Show form
        // context.challenge(createForm(context, null));

        // check for callback -> consent_cb=1
        // if callback, then context.success() and return
=======
        context.challenge(createForm(context, null));

        // check for callback -> consent_cb=1
        // if callback, then context.success() and return
        /*
>>>>>>> d160ba5 (WIP for external consent management)
        if ("1".equals(context.getHttpRequest().getUri().getQueryParameters().getFirst("consent_cb"))) {
            context.success();
            return;
        }

        ClientModel client = context.getSession().getContext().getRealm().getClientByClientId("consent-webapp-springboot");

        URI consentAppTargetUri;
        if (client.getRootUrl() != null && (client.getBaseUrl() == null || client.getBaseUrl().isEmpty())) {
            consentAppTargetUri = KeycloakUriBuilder.fromUri(client.getRootUrl()).build();
        } else {
            consentAppTargetUri = KeycloakUriBuilder.fromUri(ResolveRelative.resolveRelativeUri(context.getSession(), client.getRootUrl(), client.getBaseUrl())).build();
        }

        URI appUri = URI.create(context.getAuthenticationSession().getClientNotes().get("redirect_uri"));

        UriBuilder ub = UriBuilder.fromUri(consentAppTargetUri)
                .queryParam("redirect_uri", appUri)
                .queryParam(" client_id", context.getSession().getContext().getClient().getClientId())
                .queryParam("scope", context.getAuthenticationSession().getClientNotes().get("scope"))
                ;
        URI uri = ub.build();
        context.challenge(Response.temporaryRedirect(uri).build());
<<<<<<< HEAD
=======

         */

>>>>>>> d160ba5 (WIP for external consent management)
        // otherwise, get current url
        // redirect to consent client base url
    }

    protected Response createForm(RequiredActionContext context, Consumer<LoginFormsProvider> formCustomizer) {

        var form = context.form();
        var user = context.getUser();

        form.setAttribute(UserModel.USERNAME, user.getUsername());

        var authSession = context.getAuthenticationSession();

        Function<ScopeField, ScopeFieldBean> fun = f -> new ScopeFieldBean(f, user);

        var scopeInfo = getScopeInfo(context.getSession(), authSession, user);
        var grantedRequired = scopeInfo.getGrantedRequired();
        var grantedOptional = scopeInfo.getGrantedOptional();
        var missingRequired = scopeInfo.getMissingRequired();
        var missingOptional = scopeInfo.getMissingOptional();

        var scopes = new ArrayList<ScopeBean>();
        for (var currentScopes : List.of(grantedRequired, missingRequired, grantedOptional, missingOptional)) {
            for (var scope : currentScopes) {
                var fields = SCOPE_FIELD_MAPPING.getOrDefault(scope.getName(), List.of()).stream().map(fun).collect(toList());
                var optional = currentScopes == grantedOptional || currentScopes == missingOptional;
                var granted = currentScopes == grantedRequired || currentScopes == grantedOptional;
                scopes.add(new ScopeBean(scope, optional, granted, fields));
            }
        }

        scopes.sort(ScopeBean.DEFAULT_ORDER);

        form.setAttribute("scopes", scopes);

        if (formCustomizer != null) {
            formCustomizer.accept(form);
        }

        // use form from src/main/resources/theme-resources/templates/
        return form.createForm("select-consent-form.ftl");
    }

    @Override
    public void processAction(RequiredActionContext context) {
        // Persist user consent for client (required, optional)
        // AND/OR handle changes done for profile attributes incl. validation and persistence

        // Implementation
        // 1) Local (background sync)
        // 2) Remote call
        // 3) Redirect has happend before

        var formParameters = context.getHttpRequest().getFormParameters();

        var authSession = context.getAuthenticationSession();
        var session = context.getSession();
        var users = context.getSession().users();
        var realm = context.getRealm();
        var event = context.getEvent();
        var client = authSession.getClient();
        var user = context.getUser();

        event.client(client).user(user).event(EventType.GRANT_CONSENT);

        if (formParameters.getFirst("cancel") != null) {
            event.error(Errors.CONSENT_DENIED);
            // return to the application without consent update
            context.success();
            return;
        }

        var scopeSelection = formParameters.get("scopeSelection");
        var scopeInfo = getScopeInfo(session, authSession, user);
        var scopesToAskForConsent = new HashSet<ClientScopeModel>();

        for (var scopes : List.of( //
                scopeInfo.getGrantedRequired(), //
                scopeInfo.getMissingRequired(), //
                scopeInfo.getGrantedOptional(), //
                scopeInfo.getMissingOptional())) {
            for (var scope : scopes) {
                if (scopeSelection.contains(scope.getName())) {
                    scopesToAskForConsent.add(scope);
                }
            }
        }

        if (!scopesToAskForConsent.isEmpty()) {
            // TODO find a way to merge the existing consent with the new consent instead of replacing the existing consent
            var consentByClient = users.getConsentByClient(realm, user.getId(), client.getId());
            if (consentByClient != null) {
                users.revokeConsentForClient(realm, user.getId(), client.getId());
            }
            consentByClient = new UserConsentModel(client);

            scopesToAskForConsent.forEach(consentByClient::addGrantedClientScope);

            users.addConsent(realm, user.getId(), consentByClient);

            var grantedScopeNames = consentByClient.getGrantedClientScopes().stream().map(ClientScopeModel::getName).collect(Collectors.toList());
            grantedScopeNames.add(0, OAuth2Constants.SCOPE_OPENID);
            var scope = String.join(" ", grantedScopeNames);

            // TODO find a better way to propagate the selected scopes
            authSession.setClientNote(OIDCLoginProtocol.SCOPE_PARAM, scope);

            event.detail(OAuth2Constants.SCOPE, scope).success();
        }

        // TODO ensure that required scopes are always consented
        authSession.removeRequiredAction(getId());
        context.success();
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

    private ScopeInfo getScopeInfo(KeycloakSession session, AuthenticationSessionModel authSession, UserModel user) {
        var client = authSession.getClient();
        var requestedScopes = computeRequestedScopes(authSession, client);
        var consentByClient = session.users().getConsentByClient(authSession.getRealm(), user.getId(), client.getId());
        var missingRequired = new HashSet<>(requestedScopes.getRequired().values());
        var missingOptional = new HashSet<>(requestedScopes.getOptional().values());

        var grantedRequired = Collections.<ClientScopeModel>emptySet();
        var grantedOptional = Collections.<ClientScopeModel>emptySet();

        if (consentByClient != null) {

            grantedRequired = new HashSet<>(requestedScopes.getRequired().values());
            grantedOptional = new HashSet<>(requestedScopes.getOptional().values());

            grantedRequired.retainAll(consentByClient.getGrantedClientScopes());
            grantedOptional.retainAll(consentByClient.getGrantedClientScopes());
            missingRequired.removeAll(consentByClient.getGrantedClientScopes());
            missingOptional.removeAll(consentByClient.getGrantedClientScopes());
        }

        return ScopeInfo.builder() //
                .grantedRequired(grantedRequired) //
                .grantedOptional(grantedOptional) //
                .missingRequired(missingRequired) //
                .missingOptional(missingOptional) //
                .build();
    }

    private RequestedScopes computeRequestedScopes(AuthenticationSessionModel authSession, ClientModel client) {
        var defaultClientScopes = client.getClientScopes(true);
        var optionalClientScopes = client.getClientScopes(false);

        var requestedRequired = new HashMap<String, ClientScopeModel>();
        var requestedOptional = new HashMap<String, ClientScopeModel>();
        for (var scopeId : authSession.getClientScopes()) {
            var foundInDefaultScope = false;
            for (var scope : defaultClientScopes.values()) {
                if (scope.getId().equals(scopeId)) {
                    requestedRequired.put(scope.getName(), scope);
                    foundInDefaultScope = true;
                    break;
                }
            }
            if (!foundInDefaultScope) {
                for (var scope : optionalClientScopes.values()) {
                    if (scope.getId().equals(scopeId)) {
                        requestedOptional.put(scope.getName(), scope);
                        break;
                    }
                }
            }
        }

        return new RequestedScopes(requestedRequired, requestedOptional);
    }

    @Data
    @Builder
    static class ScopeInfo {
        private final Set<ClientScopeModel> grantedRequired;
        private final Set<ClientScopeModel> grantedOptional;

        private final Set<ClientScopeModel> missingRequired;
        private final Set<ClientScopeModel> missingOptional;
    }

    @Data
    static class RequestedScopes {

        private final Map<String, ClientScopeModel> required;
        private final Map<String, ClientScopeModel> optional;

    }
}

package com.github.thomasdarimont.keycloak.custom.auth.idpselection;

import com.github.thomasdarimont.keycloak.custom.support.ConfigUtils;
import com.google.auto.service.AutoService;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.Config;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.model.IdentityProviderBean;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Custom {@link Authenticator} that combines the {@link org.keycloak.authentication.authenticators.browser.UsernameForm}
 * with {@link org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticator}.
 */
@JBossLog
public class AcmeDynamicIdpLookupUsernameForm extends UsernamePasswordForm {

    public static final String EMAIL_DOMAIN_REGEX_IDP_CONFIG_PROPERTY = "acmeEmailDomainRegex";

    public static final String LOOKUP_REALM_NAME_CONFIG_PROPERTY = "lookupRealmName";

    public static final String LOOKUP_REALM_IDP_ALIAS_CONFIG_PROPERTY = "lookupRealmIdpAlias";

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        if (context.getUser() != null) {
            // We can skip the form when user is re-authenticating. Unless current user has some IDP set, so he can re-authenticate with that IDP
            IdentityProviderBean identityProviderBean = new IdentityProviderBean(context.getSession(), context.getRealm(), null, context);
            List<IdentityProviderBean.IdentityProvider> identityProviders = identityProviderBean.getProviders();
            if (identityProviders.isEmpty()) {
                context.success();
                return;
            }
        }
        super.authenticate(context);
    }

    @Override
    public void action(AuthenticationFlowContext context) {

        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        if (formData.containsKey("cancel")) {
            context.cancelLogin();
            return;
        }

        var session = context.getSession();
        var realm = context.getRealm();

        boolean localUserFound = validateForm(context, formData);
        if (localUserFound) {
            // local apps user found in current realm

            UserModel localUser = context.getUser();

            if (!enabledUser(context, localUser)) {
                return;
            }

            // check user for associated identity providers
            List<FederatedIdentityModel> connectedIdentityProviders = session.users().getFederatedIdentitiesStream(realm, localUser).toList();

            // there is only one linked identity provider
            if (connectedIdentityProviders.size() == 1) {

                // redirect to the associated account
                FederatedIdentityModel idpIdentity = connectedIdentityProviders.get(0);
                redirect(context, idpIdentity.getIdentityProvider(), localUser.getEmail());
                return;
            } else {
                // TODO handle user with zero or > 1 associated idps
                // TODO determine the primary IdP for users
//                log.debugf("Multiple IdPs found for user: %s", localUser.getUsername());
//                var identityProviders = new ArrayList<IdentityProviderModel>();
//                for (FederatedIdentityModel idpIdentity : connectedIdentityProviders) {
//                    // copy IdentityProviderModel to remove the hideOnLoginPage config
//                    IdentityProviderModel identityProviderByAlias = new IdentityProviderModel(realm.getIdentityProviderByAlias(idpIdentity.getIdentityProvider()));
//                    identityProviderByAlias.getConfig().remove("hideOnLoginPage");
//                    identityProviders.add(identityProviderByAlias);
//                }
//                Response response = context.form()
//                        .setAttribute("customSocial", new IdentityProviderBean(realm, session, identityProviders, context.getUriInfo().getRequestUri()))
//                        .createForm("login-idp-selection.ftl");
//                context.forceChallenge(response);

                // we could not determine a target IdP, thus we fail the authentication
                context.clearUser();
                context.attempted();
                return;
            }
        }

        var authenticatorConfig = ConfigUtils.getConfig(context.getAuthenticatorConfig(), Map.of());

        // local user NOT found
        String username = formData.getFirst(AuthenticationManager.FORM_USERNAME);
        if (username == null || username.isBlank()) {
            context.clearUser();
            context.attempted();
            return;
        }

        // try lookup in lookup realm
        String lookupRealmName = authenticatorConfig.get(LOOKUP_REALM_NAME_CONFIG_PROPERTY);
        String lookupRealmIdpAlias = authenticatorConfig.get(LOOKUP_REALM_IDP_ALIAS_CONFIG_PROPERTY);
        UserModel lookupRealmUser = findUserInLookupRealm(session, lookupRealmName, username);
        if (lookupRealmUser != null) {
            // local user found in lookup-realm, redirect user to lookup-realm for login
            log.infof("redirect user to %s via %s", lookupRealmName, lookupRealmIdpAlias);
            redirect(context, lookupRealmIdpAlias, lookupRealmUser.getEmail());
            return;
        }

        // no local user in lookup-realm found, try to identity target IdP by email
        String targetIdpAlias = resolveTargetIdpByEmailDomain(realm, username, lookupRealmIdpAlias);
        if (targetIdpAlias != null) {
            // redirect user to targetIdp
            redirect(context, targetIdpAlias, username);
            return;
        }

        // we could not found a target IdP, thus we fail the authentication here
        // fall through here, we just propagate the user not found error to the form
    }

    private String resolveTargetIdpByEmailDomain(RealmModel realm, String email, String lookupRealmIdpAlias) {

        if (!Validation.isEmailValid(email)) {
            // not an email address
            return null;
        }

        String domain = email.split("@")[1].strip();

        String idpAliasForEmail = realm.getIdentityProvidersStream().filter(idp -> {
                    Map<String, String> config = idp.getConfig();
                    if (lookupRealmIdpAlias.equals(idp.getAlias())) {
                        return false;
                    }
                    if (config == null) {
                        return false;
                    }
                    String idpEmailDomainRegex = config.get(EMAIL_DOMAIN_REGEX_IDP_CONFIG_PROPERTY);
                    return domain.matches(idpEmailDomainRegex);
                })//
                .findFirst() //
                .map(IdentityProviderModel::getAlias) //
                .orElse(null);

        return idpAliasForEmail;
    }

    @Override
    protected boolean validateForm(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        return validateUser(context, formData);
    }

    /**
     * Initiate a login through the Identity provider with the given providerId and loginHint.
     *
     * @param context
     * @param providerId
     * @param loginHint
     */
    private void redirect(AuthenticationFlowContext context, String providerId, String loginHint) {

        // adapted from org.keycloak.authentication.authenticators.browser.IdentityProviderAuthenticator.redirect

        Optional<IdentityProviderModel> idp = context.getRealm().getIdentityProvidersStream() //
                .filter(IdentityProviderModel::isEnabled) //
                .filter(identityProvider -> Objects.equals(providerId, identityProvider.getAlias())) //
                .findFirst();

        if (idp.isEmpty()) {
            log.warnf("Identity Provider not found or not enabled for realm. realm=%s provider=%s", context.getRealm().getName(), providerId);
            context.attempted();
            return;
        }

        String accessCode = new ClientSessionCode<>(context.getSession(), context.getRealm(), context.getAuthenticationSession()).getOrGenerateCode();
        String clientId = context.getAuthenticationSession().getClient().getClientId();
        String tabId = context.getAuthenticationSession().getTabId();
        URI location = Urls.identityProviderAuthnRequest(context.getUriInfo().getBaseUri(), providerId, context.getRealm().getName(), accessCode, clientId, tabId, null, loginHint);
        Response response = Response.seeOther(location).build();
        log.debugf("Redirecting to %s", providerId);
        context.forceChallenge(response);
    }

    @Override
    protected Response challenge(AuthenticationFlowContext context, MultivaluedMap<String, String> formData) {
        LoginFormsProvider forms = context.form();

        if (!formData.isEmpty()) {
            forms.setFormData(formData);
        }

        return forms.createLoginUsername();
    }

    @Override
    protected Response createLoginForm(LoginFormsProvider form) {
        return form.createLoginUsername();
    }

    @Override
    protected String getDefaultChallengeMessage(AuthenticationFlowContext context) {
        if (context.getRealm().isLoginWithEmailAllowed()) {
            return Messages.INVALID_USERNAME_OR_EMAIL;
        }
        return Messages.INVALID_USERNAME;
    }

    protected UserModel findUserInLookupRealm(KeycloakSession session, String lookupRealmName, String email) {
        var localRealm = session.realms().getRealmByName(lookupRealmName);
        var localUser = session.users().getUserByEmail(localRealm, email);
        return localUser;
    }

    @AutoService(AuthenticatorFactory.class)
    public static class Factory implements AuthenticatorFactory {

        @Override
        public String getId() {
            return "acme-auth-username-idp-select";
        }

        @Override
        public String getDisplayType() {
            return "Dynamic Idp Selection based on email domain.";
        }

        @Override
        public String getReferenceCategory() {
            return "lookup";
        }

        @Override
        public String getHelpText() {
            return "Redirects a user to a local user realm or the appropriate IdP for login";
        }

        @Override
        public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
            return REQUIREMENT_CHOICES;
        }

        @Override
        public boolean isUserSetupAllowed() {
            return false;
        }

        @Override
        public boolean isConfigurable() {
            return true;
        }

        @Override
        public List<ProviderConfigProperty> getConfigProperties() {
            var configProperties = ProviderConfigurationBuilder.create()
                    .property()
                    .name(LOOKUP_REALM_IDP_ALIAS_CONFIG_PROPERTY)
                    .label("Lookup Realm IdP Alias")
                    .helpText("IdP Alias in current realm that points to lookup realm")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("")
                    .add()

                    .property()
                    .name(LOOKUP_REALM_NAME_CONFIG_PROPERTY)
                    .label("Lookup Realm Name")
                    .helpText("Name of lookup realm")
                    .type(ProviderConfigProperty.STRING_TYPE)
                    .defaultValue("")
                    .add()

                    .build()
            ;

            return configProperties;
        }

        @Override
        public Authenticator create(KeycloakSession session) {
            return new AcmeDynamicIdpLookupUsernameForm();
        }

        @Override
        public void init(Config.Scope config) {
            // called when component is "created"
            // access to provider configuration
        }

        @Override
        public void postInit(KeycloakSessionFactory factory) {
            // called after component is discovered
        }

        @Override
        public void close() {
            // clear up state
        }
    }
}

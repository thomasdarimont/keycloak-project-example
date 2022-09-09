package com.github.thomasdarimont.keycloak.custom.idp.brokering;

import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.broker.oidc.KeycloakOIDCIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderFactory;
import org.keycloak.broker.provider.AbstractIdentityProviderMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.messages.Messages;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JBossLog
@AutoService(IdentityProviderMapper.class)
public class RestrictBrokeredUserMapper extends AbstractIdentityProviderMapper {

    public static final String PROVIDER_ID = "oidc-restrict-brokered-user";

    public static final String[] COMPATIBLE_PROVIDERS = {OIDCIdentityProviderFactory.PROVIDER_ID, KeycloakOIDCIdentityProviderFactory.PROVIDER_ID};

    private static final List<ProviderConfigProperty> configProperties;

    static {

        var properties = new ArrayList<ProviderConfigProperty>();

//        var claimsProperty = new ProviderConfigProperty();
//        claimsProperty.setName(CLAIM_PROPERTY_NAME);
//        claimsProperty.setLabel("Claims");
//        claimsProperty.setHelpText("Name and value of the claims to search for in token. You can reference nested claims using a '.', i.e. 'address.locality'. To use dot (.) literally, escape it with backslash (\\.)");
//        claimsProperty.setType(ProviderConfigProperty.MAP_TYPE);
//        configProperties.add(claimsProperty);

        configProperties = Collections.unmodifiableList(properties);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS.clone();
    }

    @Override
    public String getDisplayCategory() {
        return "Preprocessor";
    }

    @Override
    public String getDisplayType() {
        return "Acme: Restrict Brokered User";
    }

    @Override
    public String getHelpText() {
        return "Only allow LDAP federated user to login via IdP Brokering.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.copyOf(configProperties);
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return true;
    }

    public IdentityProviderMapper create(KeycloakSession session) {
        return this;
    }

    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {

        String brokerUsername = context.getUsername();

        // check if user can be found via existing user federation
        UserModel user = session.users().getUserByUsername(realm, brokerUsername);
        if (user == null) {
            // Aborted identity brokering because user was not found in this realm
            log.infof("User is not allowed to access this realm. realm=%s username=%s", realm.getName(), brokerUsername);
            // user could not be found via user federation, so we reject the user
            throw new WebApplicationException(createErrorPageResponse(session, brokerUsername));
        }
    }

    private static Response createErrorPageResponse(KeycloakSession session, String attemptedUsername) {
        var form = session.getProvider(LoginFormsProvider.class);
        form.setError(Messages.ACCESS_DENIED);
        form.setInfo("userNotAllowedToAccess", attemptedUsername);
        return form.createErrorPage(Response.Status.FORBIDDEN);
    }
}

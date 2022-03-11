package com.github.thomasdarimont.keycloak.custom.themes.login;

import com.github.thomasdarimont.keycloak.custom.config.ClientConfig;
import com.github.thomasdarimont.keycloak.custom.config.RealmConfig;
import org.keycloak.forms.login.freemarker.model.ClientBean;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import java.util.Optional;

public class AcmeUrlBean {
    private static final String ACME_SITE_URL_KEY = "acme_site_url";

    private static final String ACME_TERMS_URL_REALM_ATTRIBUTE_KEY = "acme_terms_url";
    private static final String ACME_TERMS_URL_CLIENT_ATTRIBUTE_KEY = "tosUri";

    private static final String ACME_IMPRINT_URL_KEY = "acme_imprint_url";

    private static final String ACME_PRIVACY_URL_REALM_ATTRIBUTE_KEY = "acme_privacy_url";
    public static final String ACME_PRIVACY_URL_CLIENT_ATTRIBUTE_KEY = "policyUri";

    private static final String ACME_LOGO_URL_REALM_ATTRIBUTE_KEY = "acme_logo_url";
    public static final String ACME_LOGO_URL_CLIENT_ATTRIBUTE_KEY = "logoUri";

    private static final String ACME_ACCOUNT_DELETE_URL_KEY = "acme_account_deleted_url";

    private final ClientConfig clientConfig;
    private final RealmConfig realmConfig;

    public AcmeUrlBean(KeycloakSession session) {
        this(session, null);
    }

    public AcmeUrlBean(KeycloakSession session, ClientBean clientBean) {
        RealmModel realm = session.getContext().getRealm();
        this.realmConfig = new RealmConfig(realm);

        if(clientBean != null) {
            this.clientConfig = new ClientConfig(realm.getClientByClientId(clientBean.getClientId()));
        } else {
            this.clientConfig = null;
        }
    }

    /** BEGIN: Used from freemarker */
    public String getSiteUrl() {
        return realmConfig.getValue(ACME_SITE_URL_KEY);
    }

    public String getTermsUrl() {
        return clientAttribute(ACME_TERMS_URL_CLIENT_ATTRIBUTE_KEY).orElse(realmConfig.getValue(ACME_TERMS_URL_REALM_ATTRIBUTE_KEY));
    }

    public String getPrivacyUrl() {
        return clientAttribute(ACME_PRIVACY_URL_CLIENT_ATTRIBUTE_KEY).orElse(realmConfig.getValue(ACME_PRIVACY_URL_REALM_ATTRIBUTE_KEY));
    }

    public String getImprintUrl() {
        // there is no client specific imprint
        return realmConfig.getValue(ACME_IMPRINT_URL_KEY);
    }

    public String getLogoUrl() {
        return clientAttribute(ACME_LOGO_URL_CLIENT_ATTRIBUTE_KEY).orElse(realmConfig.getValue(ACME_LOGO_URL_REALM_ATTRIBUTE_KEY));
    }

    public String getAccountDeletedUrl() {
        // there is no client specific delete url
        return realmConfig.getValue(ACME_ACCOUNT_DELETE_URL_KEY);
    }

    /** END: Used from freemarker */

    private Optional<String> clientAttribute(String key) {
        if(this.clientConfig != null) {
            return Optional.ofNullable(this.clientConfig.getValue(key));
        }
        return Optional.empty();
    }


}

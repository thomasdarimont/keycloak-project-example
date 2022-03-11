package com.github.thomasdarimont.keycloak.custom.themes.login;

import org.keycloak.forms.login.freemarker.model.ClientBean;

import java.util.Optional;

public class AcmeUrlBean {

    private static final String DEFAULT_SITE_URL = optionalOrElse(System.getenv("ACME_SITE_URL"), "http://example.org");

    private static final String DEFAULT_TERMS_URL = optionalOrElse(System.getenv("ACME_TERMS_URL"), DEFAULT_SITE_URL + "/site/terms.html");

    private static final String DEFAULT_IMPRINT_URL = optionalOrElse(System.getenv("ACME_IMPRINT_URL"), DEFAULT_SITE_URL + "/site/imprint.html");

    private static final String DEFAULT_PRIVACY_URL = optionalOrElse(System.getenv("ACME_PRIVACY_URL"), DEFAULT_SITE_URL + "/site/privacy.html");

    private static final String DEFAULT_ACCOUNT_DELETED_URL = optionalOrElse(System.getenv("ACME_ACCOUNT_DELETED_URL"), DEFAULT_SITE_URL + "/site/accountdeleted.html");

    private static final String DEFAULT_LOGO_URL = optionalOrElse(System.getenv("ACME_LOGO_URL"), null);

    private final ClientBean clientBean;

    public AcmeUrlBean() {
        this(null);
    }

    public AcmeUrlBean(ClientBean clientBean) {
        this.clientBean = clientBean;
    }

    private static String optionalOrElse(String value, String fallback) {
        return Optional.ofNullable(value).orElse(fallback);
    }

    public String getSiteUrl() {
        return DEFAULT_SITE_URL;
    }

    public String getTermsUrl() {

        if (clientBean != null) {
            return optionalOrElse(clientBean.getAttribute("tosUri"), DEFAULT_TERMS_URL);
        }

        return DEFAULT_TERMS_URL;
    }

    public String getPrivacyUrl() {

        if (clientBean != null) {
            return optionalOrElse(clientBean.getAttribute("policyUri"), DEFAULT_PRIVACY_URL);
        }

        return DEFAULT_PRIVACY_URL;
    }


    public String getImprintUrl() {
        // there is no client specific imprint
        return DEFAULT_IMPRINT_URL;
    }

    public String getLogoUrl() {

        if (clientBean != null) {
            return optionalOrElse(clientBean.getAttribute("logoUri"), DEFAULT_LOGO_URL);
        }

        return DEFAULT_LOGO_URL;
    }

    public String getAccountDeletedUrl() {
        return DEFAULT_ACCOUNT_DELETED_URL;
    }
}

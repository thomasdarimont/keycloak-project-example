package com.github.thomasdarimont.keycloak.custom.terms;

import com.google.auto.service.AutoService;
import org.keycloak.Config;
import org.keycloak.authentication.RequiredActionContext;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Optional;

@AutoService(RequiredActionFactory.class)
public class AcmeTermsAndConditionsAction implements RequiredActionProvider, RequiredActionFactory {

    public static final String PROVIDER_ID = "acme_terms_and_conditions";

    public static final String TERMS_USER_ATTRIBUTE = "acme_terms_accepted";

    public static final String TERMS_FORM_FTL = "terms.ftl";

    public static final String TERMS_DEFAULT_ID = "acme_terms";

    private static final String TERMS_CURRENT_ID = Optional.ofNullable(System.getenv("KEYCLOAK_ACME_TERMS_ID")).orElse(TERMS_DEFAULT_ID);

    private static final String TERMS_ID_SPLITTER = "@";

    @Override
    public RequiredActionProvider create(KeycloakSession session) {
        return this;
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
    public String getId() {
        return PROVIDER_ID;
    }


    @Override
    public void evaluateTriggers(RequiredActionContext context) {

        if (hasUserAcceptedCurrentTerms(context)) {
            context.getUser().removeRequiredAction(PROVIDER_ID);
        } else {
            context.getUser().addRequiredAction(PROVIDER_ID);
        }
    }

    private boolean hasUserAcceptedCurrentTerms(RequiredActionContext context) {

        String termsAttribute = context.getUser().getFirstAttribute(TERMS_USER_ATTRIBUTE);
        return termsAttribute != null  // user has accepted terms at all
                && termsAttribute.startsWith(getActiveTermsId() + TERMS_ID_SPLITTER); // user has accepted current terms
    }

    private String getActiveTermsId() {
        return TERMS_CURRENT_ID;
    }


    @Override
    public void requiredActionChallenge(RequiredActionContext context) {
        Response challenge = context.form()
                .setAttribute("terms_id", getActiveTermsId())
                .createForm(TERMS_FORM_FTL);
        context.challenge(challenge);
    }

    @Override
    public void processAction(RequiredActionContext context) {

        if (context.getHttpRequest().getDecodedFormParameters().containsKey("cancel")) {
            context.getUser().removeAttribute(TERMS_USER_ATTRIBUTE);
            context.failure();
            return;
        }

        // Record acceptance of current version of terms and conditions
        context.getUser().setAttribute(TERMS_USER_ATTRIBUTE, List.of(getActiveTermsId() + TERMS_ID_SPLITTER + Time.currentTime()));

        context.success();
    }

    @Override
    public String getDisplayText() {
        return "Acme: Terms and Conditions";
    }

    @Override
    public void close() {
        // NOOP
    }
}

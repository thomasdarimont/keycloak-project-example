package com.github.thomasdarimont.keycloak.custom.themes.login;

import org.keycloak.authentication.AuthenticationSelectionOption;
import org.keycloak.authentication.Authenticator;
import org.keycloak.forms.login.freemarker.model.AuthenticationContextBean;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AcmeLoginBean {

    private final KeycloakSession session;
    private final AuthenticationContextBean authBean;

    public AcmeLoginBean(KeycloakSession session, AuthenticationContextBean authBean) {

        this.session = session;
        this.authBean = authBean;
    }

    /**
     * Called from "select-authenticator.ftl" to narrow the available authentication options for the current user.
     *
     * @return
     */
    public List<AuthenticationSelectionOption> getAuthenticationSelections() {
        return narrowUserAuthenticationOptions(authBean.getAuthenticationSelections());
    }

    private List<AuthenticationSelectionOption> narrowUserAuthenticationOptions(List<AuthenticationSelectionOption> availableOptions) {

        KeycloakContext context = session.getContext();
        RealmModel realm = context.getRealm();
        UserModel user = context.getAuthenticationSession().getAuthenticatedUser();

        List<AuthenticationSelectionOption> elegibleOptions = availableOptions.stream()
                // filter elegible options for user
                .filter(option -> {

                    AuthenticationExecutionModel authExecution = option.getAuthenticationExecution();
                    Authenticator authenticator = session.getProvider(Authenticator.class, authExecution.getAuthenticator());
                    if (!authenticator.requiresUser()) {
                        return true;
                    }

                    boolean configured = authenticator.configuredFor(session, realm, user);
                    return configured;
                })
                // sort by priority from authentication flow
                .sorted(Comparator.comparing(option -> option.getAuthenticationExecution().getPriority()))
                .collect(Collectors.toList());

        return elegibleOptions;
    }
}

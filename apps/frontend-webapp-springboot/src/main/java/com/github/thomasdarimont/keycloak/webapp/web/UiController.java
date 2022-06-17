package com.github.thomasdarimont.keycloak.webapp.web;


import com.github.thomasdarimont.keycloak.webapp.domain.ApplicationEntry;
import com.github.thomasdarimont.keycloak.webapp.domain.CredentialEntry;
import com.github.thomasdarimont.keycloak.webapp.domain.SettingEntry;
import com.github.thomasdarimont.keycloak.webapp.domain.UserProfile;
import com.github.thomasdarimont.keycloak.webapp.keycloak.client.KeycloakClientException;
import com.github.thomasdarimont.keycloak.webapp.keycloak.client.acme.AcmeKeycloakClient;
import com.github.thomasdarimont.keycloak.webapp.support.OAuth2Accessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collection;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
class UiController {

    private final AcmeKeycloakClient acmeKeycloakClient;

    private final OAuth2Accessor oauth2Accessor;

    @GetMapping("/")

    public String showIndex(Model model) {
        return "index";
    }

    private void ensureLoggedIn(Authentication auth) {
        var oauth2AuthorizedClient = oauth2Accessor.getAuthorizedClient(auth);
        if (oauth2AuthorizedClient == null) {
            throw new AuthenticationCredentialsNotFoundException("invalid session");
        }

        oauth2Accessor.getAccessToken(auth);
    }

    @GetMapping("/profile")
    public String showProfile(Model model, Authentication auth) {

        ensureLoggedIn(auth);

        var user = (DefaultOidcUser) auth.getPrincipal();

        var profile = new UserProfile();
        profile.setFirstname(user.getGivenName());
        profile.setLastname(user.getFamilyName());
        profile.setEmail(user.getEmail());

        model.addAttribute("profile", profile);

        return "profile";
    }

    @GetMapping("/settings")
    public String showSettings(Model model, Authentication auth) {

        ensureLoggedIn(auth);

        try {
            var settingsResponse = acmeKeycloakClient.listSettings();

            var settings = settingsResponse.entrySet().stream() //
                    .map(entry -> {
                        var setting = new SettingEntry();
                        var key = entry.getKey();
                        var type = "String";
                        switch (key) {
                            case "Setting1":
                                type = "string";
                                break;
                            case "Setting2":
                                type = "boolean";
                                break;
                        }

                        setting.setName(key);
                        setting.setType(type);
                        setting.setValue(String.valueOf(entry.getValue()));
                        return setting;
                    }) //
                    .collect(Collectors.toList());

            model.addAttribute("settings", settings);
        } catch (Exception e) {
            log.error("Exception while reading user settings", e);

            model.addAttribute("message", "settings.landing.message.failed");
        }

        return "settings";
    }

    @GetMapping("/security")
    public String showSecurity(Model model, Authentication auth) {

        ensureLoggedIn(auth);

        var credentialsResponse = acmeKeycloakClient.listCredentials();

        var credentials = credentialsResponse.getCredentialInfos().values().stream() //
                .flatMap(Collection::stream) //
                .map(stringListEntry -> {
                    var credentialEntry = new CredentialEntry();
                    credentialEntry.setId(stringListEntry.getCredentialId());
                    credentialEntry.setLabel(stringListEntry.getCredentialLabel());
                    credentialEntry.setType(stringListEntry.getCredentialType());
                    return credentialEntry;
                }) //
                .collect(Collectors.toList());

        model.addAttribute("credentials", credentials);

        return "security";
    }

    @GetMapping("/applications")
    public String showApplications(Model model, Authentication auth) {

        ensureLoggedIn(auth);

        var appsResponse = acmeKeycloakClient.listApplications();

        var apps = appsResponse.getClients().stream() //
                .map(acmeApplication -> {
                    var application = new ApplicationEntry();
                    application.setClientId(acmeApplication.getClientId());
                    application.setName(acmeApplication.getClientName());
                    application.setUrl(acmeApplication.getEffectiveUrl());
                    return application;
                }) //
                .collect(Collectors.toList());

        model.addAttribute("apps", apps);

        return "applications";
    }

    @ExceptionHandler(KeycloakClientException.class)
    public String clientError(KeycloakClientException exception) {
        log.error("Client failed with status {} and body {}", exception.getStatusCode(), exception.getErrorBody());
        return "profile";
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public String sessionNotFound(AuthenticationCredentialsNotFoundException exception) {
        SecurityContextHolder.clearContext();
        return "redirect:profile";
    }

}

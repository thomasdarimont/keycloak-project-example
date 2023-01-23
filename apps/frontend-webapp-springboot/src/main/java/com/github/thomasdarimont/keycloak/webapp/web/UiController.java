package com.github.thomasdarimont.keycloak.webapp.web;


import com.github.thomasdarimont.keycloak.webapp.domain.ApplicationEntry;
import com.github.thomasdarimont.keycloak.webapp.domain.CredentialEntry;
import com.github.thomasdarimont.keycloak.webapp.domain.SettingEntry;
import com.github.thomasdarimont.keycloak.webapp.domain.UserProfile;
import com.github.thomasdarimont.keycloak.webapp.support.OAuth2AuthorizedClientAccessor;
import com.github.thomasdarimont.keycloak.webapp.support.keycloakclient.KeycloakClient;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
class UiController {

    private final OAuth2AuthorizedClientAccessor oauth2AuthorizedClientAccessor;

    private final KeycloakClient keycloakClient;

    @GetMapping("/")
    public String showIndex(Model model) {
        return "index";
    }

    @GetMapping("/profile")
    public String showProfile(Model model, Authentication auth) {

        var authorizedClient = oauth2AuthorizedClientAccessor.getOAuth2AuthorizedClient(auth);
        if (authorizedClient == null) {
            SecurityContextHolder.clearContext();
            return "redirect:";
        }

        var principal = (DefaultOidcUser) auth.getPrincipal();
        var profile = buildUserProfile(authorizedClient, principal);
        model.addAttribute("profile", profile);

        return "profile";
    }

    private UserProfile buildUserProfile(OAuth2AuthorizedClient oAuth2AuthorizedClient, DefaultOidcUser oidcUser) {

        var keycloakUserInfo = keycloakClient.userInfo(oAuth2AuthorizedClient, oidcUser.getIdToken());
        var profile = new UserProfile();
        profile.setFirstname(keycloakUserInfo.getFirstname());
        profile.setLastname(keycloakUserInfo.getLastname());
        profile.setEmail(keycloakUserInfo.getEmail());
        profile.setPhoneNumber(keycloakUserInfo.getPhoneNumber());
        return profile;
    }

    @GetMapping("/settings")
    public String showSettings(Model model, Authentication auth) {

        var authorizedClient = oauth2AuthorizedClientAccessor.getOAuth2AuthorizedClient(auth);
        if (authorizedClient == null) {
            SecurityContextHolder.clearContext();
            return "redirect:settings";
        }

        var setting1 = new SettingEntry();
        setting1.setName("setting1");
        setting1.setValue("value1");
        setting1.setType("string");

        var setting2 = new SettingEntry();
        setting2.setName("setting2");
        setting2.setValue("on");
        setting2.setType("boolean");

        var settings = List.of(setting1, setting2);

        model.addAttribute("settings", settings);

        return "settings";
    }

    @GetMapping("/security")
    public String showSecurity(Model model, Authentication auth) {

        var authorizedClient = oauth2AuthorizedClientAccessor.getOAuth2AuthorizedClient(auth);
        if (authorizedClient == null) {
            SecurityContextHolder.clearContext();
            return "redirect:security";
        }

        var credential1 = new CredentialEntry();
        credential1.setId("cred1");
        credential1.setLabel("value1");
        credential1.setType("password");

        var credential2 = new CredentialEntry();
        credential2.setId("cred2");
        credential2.setLabel("value2");
        credential2.setType("totp");

        var credentials = List.of(credential1, credential2);

        model.addAttribute("credentials", credentials);

        return "security";
    }

    @GetMapping("/applications")
    public String showApplications(Model model, Authentication auth) {

        var authorizedClient = oauth2AuthorizedClientAccessor.getOAuth2AuthorizedClient(auth);
        if (authorizedClient == null) {
            SecurityContextHolder.clearContext();
            return "redirect:applications";
        }

        var appEntry1 = new ApplicationEntry();
        appEntry1.setClientId("app1");
        appEntry1.setName("App 1");
        appEntry1.setUrl("http://localhost/app1");

        var appEntry2 = new ApplicationEntry();
        appEntry2.setClientId("app2");
        appEntry2.setName("App 2");
        appEntry2.setUrl("http://localhost/app2");

        var apps = List.of(appEntry1, appEntry2);

        model.addAttribute("apps", apps);

        return "applications";
    }
}

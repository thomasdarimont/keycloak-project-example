package com.github.thomasdarimont.keycloak.webapp.web;


import com.github.thomasdarimont.keycloak.webapp.domain.ApplicationEntry;
import com.github.thomasdarimont.keycloak.webapp.domain.CredentialEntry;
import com.github.thomasdarimont.keycloak.webapp.domain.SettingEntry;
import com.github.thomasdarimont.keycloak.webapp.domain.UserProfile;
import com.github.thomasdarimont.keycloak.webapp.support.TokenAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
class UiController {

    private final TokenAccessor tokenAccessor;

    @GetMapping("/")

    public String showIndex(Model model) {
        return "index";
    }

    @GetMapping("/profile")
    public String showProfile(Model model, Authentication auth) {

        OAuth2AccessToken accessToken = tokenAccessor.getAccessToken(auth);

        var oauth = (OAuth2AuthenticationToken)auth;
        var oauthUser = (DefaultOidcUser)oauth.getPrincipal();

        var profile = new UserProfile();
        profile.setFirstname(oauthUser.getGivenName());
        profile.setLastname(oauthUser.getFamilyName());
        profile.setEmail(oauthUser.getEmail());

        model.addAttribute("profile", profile);

        return "profile";
    }

    @GetMapping("/settings")
    public String showSettings(Model model) {

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
    public String showSecurity(Model model) {

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
    public String showApplications(Model model) {

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

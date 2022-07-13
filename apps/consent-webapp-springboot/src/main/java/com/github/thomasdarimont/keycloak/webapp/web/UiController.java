package com.github.thomasdarimont.keycloak.webapp.web;


import com.github.thomasdarimont.keycloak.webapp.domain.UserProfile;
import com.github.thomasdarimont.keycloak.webapp.support.TokenAccessor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.WebRequest;

@Controller
@RequiredArgsConstructor
class UiController {

    private final TokenAccessor tokenAccessor;

    @GetMapping("/")
    public String showIndex(Model model, WebRequest webRequest) {

        String redirectUri = webRequest.getParameter("redirect_uri");
        redirectUri += "&consent_cb=1";
        // TODO validate required_uri
        model.addAttribute("redirectUri", redirectUri);

        return "index";
    }

    @GetMapping("/profile")
    public String showProfile(Model model, Authentication auth) {

        OAuth2AccessToken accessToken = tokenAccessor.getAccessToken(auth);

        var profile = new UserProfile();
        profile.setFirstname("Thomas");
        profile.setLastname("Darimont");
        profile.setEmail("thomas.darimont@gmail.com");

        model.addAttribute("profile", profile);

        return "profile";
    }
}

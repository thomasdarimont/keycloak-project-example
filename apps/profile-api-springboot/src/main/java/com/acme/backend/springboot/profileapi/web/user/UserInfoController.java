package com.acme.backend.springboot.profileapi.web.user;

import com.acme.backend.springboot.profileapi.profile.ConsentAwareUserProfileService;
import com.acme.backend.springboot.profileapi.profile.model.UserProfileRepository;
import com.acme.backend.springboot.profileapi.support.oauth2.TokenAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Set;


/**
 * Custom UserInfo endpoint is intended as a replacement for Keycloak's Userinfo endpoint.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
class UserInfoController {

    private final HttpServletRequest httpRequest;

    private final TokenAccessor tokenAccessor;

    private final UserProfileRepository userProfileRepository;

    private final ConsentAwareUserProfileService profileService;

    @GetMapping
    public Object renderProfileDataAsUserInfoResponse() {

        log.info("### Accessing {}", httpRequest.getRequestURI());

        var accessToken = tokenAccessor.getCurrentAccessToken().orElseThrow();

        var clientId = accessToken.<String>getClaim("azp");
        var userId = accessToken.getSubject();
        var scopes = Set.of(accessToken.<String>getClaim("scope").split(" "));

        var userInfo = new LinkedHashMap<String, Object>();
        userInfo.put("sub", userId);

        // TODO add additional claims for internal attributes (email_verified, phone_number_verified, etc.)

        addProfileAttributesToUserInfo(clientId, userId, scopes, userInfo);

        // Example userinfo response from Keycloak
        /*

PROFILE USERINFO SETTINGS SECURITY APPS LOGOUT
{
    "sub": "befb2e1f-6a1f-42ee-89c6-7a983aee4368",
    "resource_access": {
        "account": {
            "roles": [
                "manage-account",
                "manage-account-links",
                "view-profile"
            ]
        }
    },
    "email_verified": true,
    "realm_access": {
        "roles": [
            "default-roles-acme-internal",
            "offline_access",
            "acme-user"
        ]
    },
    "name": "Theo Tester",
    "preferred_username": "tester",
    "given_name": "Theo",
    "locale": "en",
    "family_name": "Tester",
    "email": "tester@local"
}
         */

        return userInfo;
    }

    private void addProfileAttributesToUserInfo(String clientId, String userId, Set<String> scopes, LinkedHashMap<String, Object> userInfo) {
        var profileAttributes = profileService.getProfileAttributes(clientId, scopes, userId);
        for (var entry : profileAttributes.entrySet()) {
            entry.getValue().forEach(attr -> userInfo.put(attr.getName(), attr.getValue()));
        }
    }
}


package com.acme.backend.springboot.profileapi.web.user;

import com.acme.backend.springboot.profileapi.profile.ConsentAwareUserProfileService;
import com.acme.backend.springboot.profileapi.profile.model.UserProfile;
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

import static com.acme.backend.springboot.profileapi.profile.schema.UserProfileSchema.PersonAttributes;
import static com.acme.backend.springboot.profileapi.profile.schema.UserProfileSchema.Scope;


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

        var profile = userProfileRepository.getProfileByUserId(userId);

        var userInfo = new LinkedHashMap<String, Object>();
        userInfo.put("sub", userId);

        addProfileAttributesToUserInfo(clientId, profile, scopes, userInfo);

        if (scopes.contains(Scope.NAME)) {
            var displayName = getDisplayName(userInfo);
            if (!displayName.isEmpty()) {
                userInfo.put("name", displayName);
            }
        }

        if (scopes.contains(Scope.EMAIL)) {
            userInfo.put("email_verified", profile.isEmailVerified());
        }

        if (scopes.contains(Scope.PHONE)) {
            userInfo.put("phone_number_verified", profile.isPhoneNumberVerified());
        }

        return userInfo;
    }

    private String getDisplayName(LinkedHashMap<String, Object> userInfo) {
        return (userInfo.getOrDefault(PersonAttributes.FIRSTNAME.getClaimName(), "") + " " + userInfo.getOrDefault(PersonAttributes.LASTNAME.getClaimName(), "")).trim();
    }

    private void addProfileAttributesToUserInfo(String clientId, UserProfile profile, Set<String> scopes, LinkedHashMap<String, Object> userInfo) {
        var profileAttributes = profileService.getProfileAttributes(clientId, scopes, profile);
        for (var entry : profileAttributes.entrySet()) {
            entry.getValue().forEach(attr -> userInfo.put(attr.getClaimName(), attr.getValue()));
        }
    }

// Example userinfo response from Keycloak
/*
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
}


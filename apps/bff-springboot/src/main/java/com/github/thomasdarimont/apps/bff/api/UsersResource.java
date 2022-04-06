package com.github.thomasdarimont.apps.bff.api;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
class UsersResource {

    @GetMapping("/me")
    Object userInfo(Authentication auth) {
        return auth.getAuthorities().stream() //
                .filter(OidcUserAuthority.class::isInstance) //
                .map(authority -> (OidcUserAuthority) authority).map(OidcUserAuthority::getUserInfo) //
                .findFirst() //
                .orElseGet(() -> OidcUserInfo.builder().claim("error", "UserInfoMissing").build());
    }
}

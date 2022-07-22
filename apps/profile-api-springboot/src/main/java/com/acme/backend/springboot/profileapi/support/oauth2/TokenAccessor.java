package com.acme.backend.springboot.profileapi.support.oauth2;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TokenAccessor {

    public Optional<Jwt> getCurrentAccessToken() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return Optional.empty();
        }

        if (!(authentication instanceof JwtAuthenticationToken)) {
            return Optional.empty();
        }

        Jwt accessToken = ((JwtAuthenticationToken) authentication).getToken() ;
        return Optional.of(accessToken);
    }
}

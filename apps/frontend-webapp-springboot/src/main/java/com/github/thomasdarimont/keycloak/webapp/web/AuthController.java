package com.github.thomasdarimont.keycloak.webapp.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.github.thomasdarimont.keycloak.webapp.support.TokenIntrospector;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
class AuthController {

    private final TokenIntrospector tokenIntrospector;

    @GetMapping("/auth/check-session")
    public ResponseEntity<?> checkSession(Authentication auth) {

        var introspectionResult = tokenIntrospector.introspectToken(auth);

        if (introspectionResult == null || !introspectionResult.isActive()) {
            SecurityContextHolder.clearContext();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok().build();
    }

    @Data
    static class IntrospectionResponse {

        private boolean active;

        private Map<String, Object> data = new HashMap<>();

        @JsonAnySetter
        public void setDataEntry(String key, Object value) {
            data.put(key, value);
        }
    }
}

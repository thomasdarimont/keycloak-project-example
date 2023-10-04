package com.github.thomasdarimont.apps.bff3.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.github.thomasdarimont.apps.bff3.oauth.TokenIntrospector;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
class AuthResource {


    private final TokenIntrospector tokenIntrospector;

    @GetMapping("/check-session")
    public ResponseEntity<?> checkSession(Authentication auth, HttpServletRequest request) throws ServletException {

        var introspectionResult = tokenIntrospector.introspectToken(auth, request);

        if (introspectionResult == null || !introspectionResult.isActive()) {
//            SecurityContextHolder.clearContext();
            request.logout();
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

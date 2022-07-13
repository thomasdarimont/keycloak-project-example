package com.github.thomasdarimont.keycloak.webapp.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.github.thomasdarimont.keycloak.webapp.support.TokenIntrospector;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final TokenIntrospector tokenIntrospector;

    private final ClientRegistrationRepository clientRegistrationRepository;

    private final AuthorizationRequestRepository<OAuth2AuthorizationRequest> authorizationRequestRepository;

    /**
     * Init anonymous registration via: https://apps.acme.test:4633/webapp/auth/register
     *
     * @param request
     * @param response
     * @return
     */
    @GetMapping("/auth/register")
    public ResponseEntity<?> register(HttpServletRequest request, HttpServletResponse response) {

        var resolver = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, request.getContextPath());
        resolver.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());

        var authzRequest = resolver //
                .resolve(request, "keycloak");

        authorizationRequestRepository.saveAuthorizationRequest(authzRequest, request, response);

        var registerUriString = authzRequest.getAuthorizationRequestUri() //
                .replaceFirst("/openid-connect/auth", "/openid-connect/registrations");

        var registerUri = URI.create(registerUriString);

        return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).location(registerUri).build();
    }

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

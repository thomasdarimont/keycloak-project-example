package com.acme.backend.springboot.users.support.access;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import java.util.function.Supplier;

/**
 * Example for generic custom access checks on request level.
 */
@Slf4j
public class AccessController {

    private static final AuthorizationDecision GRANTED = new AuthorizationDecision(true);
    private static final AuthorizationDecision DENIED = new AuthorizationDecision(false);

    public static AuthorizationDecision checkAccess(Supplier<Authentication> authentication, RequestAuthorizationContext requestContext) {

        var auth = authentication.get();
        log.info("Check access for username={} path={}", auth.getName(), requestContext.getRequest().getRequestURI());

        return GRANTED;
    }
}

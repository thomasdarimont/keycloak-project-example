package com.acme.backend.springboot.users.config;

import java.util.function.Supplier;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import lombok.extern.slf4j.Slf4j;

/**
 * Example for generic custom access checks on request level.
 */
@Slf4j
public class AccessController {

	private static final AuthorizationDecision GRANTED = new AuthorizationDecision(true);
	private static final AuthorizationDecision DENIED = new AuthorizationDecision(false);

	public static AuthorizationDecision checkAccess(Supplier<Authentication> authentication, RequestAuthorizationContext requestContext) {

		var auth = authentication.get();
		if (auth == null) {
			log.warn("Authentication provider returned null authentication");
			return DENIED;
		}
		log.info("Check access for username={} path={}", auth.getName(), requestContext.getRequest().getRequestURI());
		return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter("ROLE_ACCESS"::equals).count() > 0 ? GRANTED : DENIED;
	}
}

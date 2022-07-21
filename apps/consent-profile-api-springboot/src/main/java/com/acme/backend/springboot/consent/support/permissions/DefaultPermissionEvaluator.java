package com.acme.backend.springboot.consent.support.permissions;

import com.acme.backend.springboot.consent.config.MethodSecurityConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

/**
 * Custom {@link PermissionEvaluator} for method level permission checks.
 *
 * @see MethodSecurityConfig
 */
@Slf4j
@Component
@RequiredArgsConstructor
class DefaultPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication auth, Object targetDomainObject, Object permission) {
        log.info("check permission user={} target={} permission={}", auth.getName(), targetDomainObject, permission);

        // TODO implement sophisticated permission check here
        return true;
    }

    @Override
    public boolean hasPermission(Authentication auth, Serializable targetId, String targetType, Object permission) {
        DomainObjectReference dor = new DomainObjectReference(targetType, targetId.toString());
        return hasPermission(auth, dor, permission);
    }
}

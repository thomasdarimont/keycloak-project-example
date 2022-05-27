package com.acme.backend.springboot.users.support.permissions;

import lombok.Data;

/**
 * Defines a single domain object by a type and name to look up
 */
@Data
public class DomainObjectReference {

    private final String type;

    private final String id;
}

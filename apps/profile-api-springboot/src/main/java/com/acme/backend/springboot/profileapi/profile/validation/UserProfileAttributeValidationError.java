package com.acme.backend.springboot.profileapi.profile.validation;

import lombok.Data;

@Data
public class UserProfileAttributeValidationError {

    private final String type;

    private final String attributeName;
}

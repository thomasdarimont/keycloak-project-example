package com.acme.backend.springboot.profileapi.profile.validation;

import org.springframework.util.ObjectUtils;

public interface UserProfileAttributeValidations {

    UserProfileAttributeValidation NOT_EMPTY = ((profile, attribute, newValue, errors) -> {

        boolean valid = !ObjectUtils.isEmpty(newValue);

        if (!valid) {
            errors.addError("NOT_EMPTY", attribute);
        }

        return valid;
    });
}

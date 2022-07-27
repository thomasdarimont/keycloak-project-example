package com.acme.backend.springboot.profileapi.profile.validation;

import com.acme.backend.springboot.profileapi.profile.schema.UserProfileAttribute;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserProfileAttributeValidationErrors {

    private final List<UserProfileAttributeValidationError> errors = new ArrayList<>();

    public void addError(String type, UserProfileAttribute attribute) {
        errors.add(new UserProfileAttributeValidationError(type, attribute.getName()));
    }
}

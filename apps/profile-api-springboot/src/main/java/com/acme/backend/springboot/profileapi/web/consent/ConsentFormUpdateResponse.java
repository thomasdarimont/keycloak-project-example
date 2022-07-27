package com.acme.backend.springboot.profileapi.web.consent;

import com.acme.backend.springboot.profileapi.profile.validation.UserProfileAttributeValidationError;
import lombok.Data;

import java.util.List;

@Data
public class ConsentFormUpdateResponse {

    // Attribute level validation error messages?
    private final List<UserProfileAttributeValidationError> errors;

    // Form level error validation  messages?

}

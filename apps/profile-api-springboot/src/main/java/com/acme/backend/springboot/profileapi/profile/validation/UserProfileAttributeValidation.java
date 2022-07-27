package com.acme.backend.springboot.profileapi.profile.validation;

import com.acme.backend.springboot.profileapi.profile.model.UserProfile;
import com.acme.backend.springboot.profileapi.profile.schema.UserProfileAttribute;

import java.util.List;

public interface UserProfileAttributeValidation {

    boolean test(UserProfile profile, UserProfileAttribute attribute, String newValue, UserProfileAttributeValidationErrors errors);
}

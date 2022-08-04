package com.acme.backend.springboot.profileapi.profile;

import com.acme.backend.springboot.profileapi.profile.model.UserConsentRepository;
import com.acme.backend.springboot.profileapi.profile.model.UserProfile;
import com.acme.backend.springboot.profileapi.profile.model.UserProfileRepository;
import com.acme.backend.springboot.profileapi.profile.schema.UserProfileAttribute;
import com.acme.backend.springboot.profileapi.profile.schema.UserProfileSchemaRepository;
import com.acme.backend.springboot.profileapi.profile.validation.UserProfileAttributeValidationErrors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConsentAwareUserProfileService {

    private final UserProfileSchemaRepository userProfileSchemaRepository;

    private final UserProfileRepository userProfileRepository;

    private final UserConsentRepository userConsentRepository;

    public Map<String, List<PopulatedUserProfileAttribute>> getProfileAttributes(UserProfile profile, String clientId, Set<String> scopes) {

        var profileSchemaAttributes = getProfileSchemaAttributes(clientId, scopes);
        if (profileSchemaAttributes.isEmpty()) {
            return Map.of();
        }

        var result = new HashMap<String, List<PopulatedUserProfileAttribute>>();

        for (var entry : profileSchemaAttributes.entrySet()) {
            var scope = entry.getKey();
            var attributes = new ArrayList<PopulatedUserProfileAttribute>();
            for (var attribute : entry.getValue()) {
                var populatedAttribute = populate(attribute, profile);
                attributes.add(populatedAttribute);
            }
            result.put(scope, attributes);
        }

        return result;
    }

    public UserProfile getUserProfile(String userId) {
        return userProfileRepository.getProfileByUserId(userId);
    }

    private Map<String, List<UserProfileAttribute>> getProfileSchemaAttributes(String clientId, Set<String> scopes) {

        var profileSchema = userProfileSchemaRepository.getProfileSchema(clientId);
        return profileSchema.getScopeAttributeMapping(scopes);
    }

    private PopulatedUserProfileAttribute populate(UserProfileAttribute attribute, UserProfile profile) {

        // TODO: discuss do we need support for computing attribute values depending on the clientId / scope?

        var value = Optional //
                .ofNullable(attribute.getAccessor()) //
                .map(extractor -> extractor.apply(profile)) //
                .orElseGet(attribute::getDefaultValue);

        return new PopulatedUserProfileAttribute(attribute, value);
    }


    public void updateProfileAttributes(UserProfile profile, String clientId, Set<String> scopes, //
                                        Map<String, String> profileUpdate, //
                                        UserProfileAttributeValidationErrors validationErrors) {

        userConsentRepository.updateConsent(profile.getId(), clientId, scopes);

        var profileSchema = userProfileSchemaRepository.getProfileSchema(clientId);

        Map<String, Runnable> attributeUpdates = new HashMap<>();

        for (var entry : profileSchema.getScopeAttributeMapping(scopes).entrySet()) {
            var scope = entry.getKey();
            var attributes = entry.getValue();

            for (var attribute : attributes) {

                if (attribute.isReadOnly()) {
                    continue;
                }

                if (!profileUpdate.containsKey(attribute.getName())) {
                    continue;
                }

                var newValue = profileUpdate.get(attribute.getName());
                if (!attribute.isValid(profile, newValue, validationErrors)) {
                    continue;
                }

                // collect attribute updates
                attributeUpdates.put(scope + ":" + attribute.getName(), () -> {
                    attribute.update(profile, newValue);
                });
            }
        }

        if (!validationErrors.isEmpty()) {
            return;
        }

        // apply attribute updates
        attributeUpdates.values().forEach(Runnable::run);

        // update internal attributes
        if (profileUpdate.containsKey("email_verified")) {
            profile.setEmailVerified(Boolean.parseBoolean(profileUpdate.get("email_verified")));
        }
    }
}

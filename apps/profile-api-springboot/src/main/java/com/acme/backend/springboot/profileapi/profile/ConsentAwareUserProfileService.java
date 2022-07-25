package com.acme.backend.springboot.profileapi.profile;

import com.acme.backend.springboot.profileapi.profile.model.UserProfile;
import com.acme.backend.springboot.profileapi.profile.model.UserProfileRepository;
import com.acme.backend.springboot.profileapi.profile.schema.UserProfileAttribute;
import com.acme.backend.springboot.profileapi.profile.schema.UserProfileSchemaRepository;
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

    public Map<String, List<PopulatedUserProfileAttribute>> getProfileAttributes(String clientId, Set<String> scopes, UserProfile profile) {

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

    public Map<String, List<PopulatedUserProfileAttribute>> getProfileAttributes(String clientId, Set<String> scopes, String userId) {
        return getProfileAttributes(clientId, scopes, userProfileRepository.getProfileByUserId(userId));
    }

    private Map<String, List<UserProfileAttribute>> getProfileSchemaAttributes(String clientId, Set<String> scopes) {

        var profileSchema = userProfileSchemaRepository.getProfileSchema(clientId);
        return profileSchema.getScopeAttributeMapping(scopes);
    }

    private PopulatedUserProfileAttribute populate(UserProfileAttribute attribute, UserProfile profile) {

        var value = Optional //
                .ofNullable(attribute.getAccessor()) //
                .map(extractor -> extractor.apply(profile)) //
                .orElseGet(attribute::getDefaultValue);

        return new PopulatedUserProfileAttribute(attribute, value);
    }


}

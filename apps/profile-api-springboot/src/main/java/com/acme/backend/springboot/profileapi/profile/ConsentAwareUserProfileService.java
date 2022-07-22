package com.acme.backend.springboot.profileapi.profile;

import com.acme.backend.springboot.profileapi.profile.model.UserProfile;
import com.acme.backend.springboot.profileapi.profile.model.UserProfileRepository;
import com.acme.backend.springboot.profileapi.profile.schema.UserProfileAttribute;
import com.acme.backend.springboot.profileapi.profile.schema.UserProfileSchemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ConsentAwareUserProfileService {

    private final UserProfileSchemaRepository userProfileSchemaRepository;

    private final UserProfileRepository userProfileRepository;

    public Map<String, List<PopulatedUserProfileAttribute>> getProfileAttributes(String clientId, Set<String> scopes, String userId) {

        var profile = userProfileRepository.getProfileByUserId(userId);
        if (profile == null) {
            return Map.of();
        }

        var profileSchemaAttributes = getProfileSchemaAttributes(clientId, scopes);
        if (profileSchemaAttributes.isEmpty()) {
            return Map.of();
        }

        var result = new LinkedHashMap<String, List<PopulatedUserProfileAttribute>>();
        for (var entry : profileSchemaAttributes.entrySet()) {
            var scope = entry.getKey();
            var attributes = entry.getValue().stream().map(attr -> populate(attr, profile)).toList();
            result.put(scope, attributes);
        }

        return result;
    }

    private Map<String, List<UserProfileAttribute>> getProfileSchemaAttributes(String clientId, Set<String> scopes) {

        var profileSchema = userProfileSchemaRepository.getProfileAttributes(clientId);

        // TODO use scopes to filter required data
        return profileSchema.getScopeAttributeMapping(scopes);
    }

    private PopulatedUserProfileAttribute populate(UserProfileAttribute attribute, UserProfile profile) {

        var populators = getProfileAttributePopulators();

        var value = Optional //
                .ofNullable(populators.get(attribute.getName())) //
                .map(fun -> fun.apply(profile)) //
                .orElseGet(attribute::getDefaultValue);

        return new PopulatedUserProfileAttribute(attribute, value);
    }

    private Map<String, Function<UserProfile, String>> getProfileAttributePopulators() {
        return Map.ofEntries(//
                Map.entry("firstName", UserProfile::getFirstName), //
                Map.entry("lastName", UserProfile::getLastName), //
                Map.entry("email", UserProfile::getEmail), //
                Map.entry("salutation", UserProfile::getSalutation), //
                Map.entry("title", UserProfile::getTitle), //
                Map.entry("birthdate", UserProfile::getBirthdate), //
                Map.entry("phoneNumber", UserProfile::getPhoneNumber), //
                Map.entry("address.street", UserProfile::getAddressStreet), //
                Map.entry("address.careOf", UserProfile::getAddressCareOf), //
                Map.entry("address.postalCode", UserProfile::getAddressPostalCode), //
                Map.entry("address.region", UserProfile::getAddressRegion), //
                Map.entry("address.country", UserProfile::getAddressCountry) //
        );
    }

}

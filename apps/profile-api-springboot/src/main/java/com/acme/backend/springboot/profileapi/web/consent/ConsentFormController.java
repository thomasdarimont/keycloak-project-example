package com.acme.backend.springboot.profileapi.web.consent;

import com.acme.backend.springboot.profileapi.profile.ConsentAwareUserProfileService;
import com.acme.backend.springboot.profileapi.profile.validation.UserProfileAttributeValidationErrors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Set;


/**
 * Handle Consent Form
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/consentForm")
class ConsentFormController {

    private final HttpServletRequest httpRequest;

    private final ConsentAwareUserProfileService profileService;

    // get the metadata of the consent-form and if the form should be shown at all
    @GetMapping("/{userId}") // ?clientId=daba&scope=email+name
    public ResponseEntity<ConsentFormDataResponse> getProfileAttributesForConsentForm(@PathVariable("userId") String userId, ConsentFormDataRequest dataRequest) {

        log.info("### Get Profile attributes for consent form: {}", httpRequest.getRequestURI());
        var scopes = Set.of(dataRequest.getScope().split("(\\s|\\+)"));
        var clientId = dataRequest.getClientId();

        var attributes = profileService.getProfileAttributes(userId, clientId, scopes);

        return ResponseEntity.ok().body(new ConsentFormDataResponse(attributes));
    }

    // receive the update from the consent-form
    @PostMapping("/{userId}")
    public ResponseEntity<ConsentFormUpdateResponse> updateForm(@PathVariable("userId") String userId, ConsentFormDataRequest dataRequest, @RequestBody Map<String, String> profileUpdate) {

        log.info("### Update Profile attributes from consent form: {}", httpRequest.getRequestURI());

        var scopes = Set.of(dataRequest.getScope().split("(\\s|\\+)"));
        var clientId = dataRequest.getClientId();

        var validationErrors = new UserProfileAttributeValidationErrors();
        profileService.updateProfileAttributes(userId, clientId, scopes, profileUpdate, validationErrors);

        var errors = validationErrors.getErrors();
        if (!CollectionUtils.isEmpty(errors)) {
            return ResponseEntity.badRequest().body(new ConsentFormUpdateResponse(errors));
        }

        return ResponseEntity.ok(new ConsentFormUpdateResponse(errors));
    }

    //Delete?
    //Not required. Consent is removed via Account App

}


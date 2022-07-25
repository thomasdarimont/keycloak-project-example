package com.acme.backend.springboot.profileapi.web.consent;

import com.acme.backend.springboot.profileapi.profile.ConsentAwareUserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public Object getProfileAttributesForConsentForm(@PathVariable("userId") String userId, ConsentFormDataRequest dataRequest) {

        log.info("### Get Profile attributes for consent form: {}", httpRequest.getRequestURI());
        var scopes = Set.of(dataRequest.getScope().split("(\\s|\\+)"));
        var clientId = dataRequest.getClientId();

        var attributes = profileService.getProfileAttributes(clientId, scopes, userId);

        return new ConsentFormDataResponse(attributes);
    }

    // receive the update from the consent-form
    @PostMapping("/{userId}")
    public Object updateForm(@PathVariable("userId") String userId, @RequestBody ConsentFormUpdateRequest updateRequest) {

        // check if profile update is allowed
        // read profile attributes to update from request
        // validate new profile values -> collect validation errors and return

        // update profile
        // update consented scopes by client,e.g. -> user,client,scopes,timestamp

        // return success response
        // after this requests for userinfo should reflect the new userprofile values (eventually?)
        return Map.of();
    }

    //Delete?
    //Not required. Consent is removed via Account App

}


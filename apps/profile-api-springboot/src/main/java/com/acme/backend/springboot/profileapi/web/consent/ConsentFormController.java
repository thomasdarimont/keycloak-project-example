package com.acme.backend.springboot.profileapi.web.consent;

import com.acme.backend.springboot.profileapi.profile.ConsentAwareUserProfileService;
import com.acme.backend.springboot.profileapi.profile.validation.UserProfileAttributeValidationErrors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(summary = "Get profile data for consent")
    @ApiResponses(value = { //
            @ApiResponse(responseCode = "200", description = "ConsentFormDataResponse found", content = { //
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ConsentFormDataResponse.class)) //
            }), //
            @ApiResponse(responseCode = "400", description = "Invalid Request", content = @Content), //
            @ApiResponse(responseCode = "404", description = "UserProfile not found", content = @Content) //
    })
    @GetMapping("/{userId}") // ?clientId=daba&scope=email+name
    public ResponseEntity<ConsentFormDataResponse> getProfileAttributesForConsentForm(@PathVariable("userId") String userId, ConsentFormDataRequest dataRequest) {

        log.info("### Get Profile attributes for consent form: {}", httpRequest.getRequestURI());
        var scopes = Set.of(dataRequest.getScope().split("(\\s|\\+)"));
        var clientId = dataRequest.getClientId();

        var profile = profileService.getUserProfile(userId);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }

        var attributes = profileService.getProfileAttributes(profile, clientId, scopes);

        return ResponseEntity.ok().body(new ConsentFormDataResponse(attributes));
    }

    // receive the update from the consent-form
    @Operation(summary = "Get update profile data from consent")
    @ApiResponses(value = { //
            @ApiResponse(responseCode = "200", description = "Profile data updated for consent", content = { //
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ConsentFormUpdateResponse.class)) //
            }), //
            @ApiResponse(responseCode = "400", description = "Invalid Request", content = { //
                    @Content(mediaType = "application/json", schema = @Schema(implementation = ConsentFormUpdateResponse.class)) //
            }), //
            @ApiResponse(responseCode = "404", description = "UserProfile not found", content = @Content) //
    })

    @PostMapping("/{userId}")
    public ResponseEntity<ConsentFormUpdateResponse> updateForm(@PathVariable("userId") String userId, ConsentFormDataRequest dataRequest, @RequestBody Map<String, String> profileUpdate) {

        log.info("### Update Profile attributes from consent form: {}", httpRequest.getRequestURI());

        var scopes = Set.of(dataRequest.getScope().split("(\\s|\\+)"));
        var clientId = dataRequest.getClientId();

        var profile = profileService.getUserProfile(userId);
        if (profile == null) {
            return ResponseEntity.notFound().build();
        }

        var validationErrors = new UserProfileAttributeValidationErrors();
        profileService.updateProfileAttributes(profile, clientId, scopes, profileUpdate, validationErrors);

        var errors = validationErrors.getErrors();
        if (!CollectionUtils.isEmpty(errors)) {
            return ResponseEntity.badRequest().body(new ConsentFormUpdateResponse(errors));
        }

        return ResponseEntity.ok(new ConsentFormUpdateResponse(errors));
    }

    //Delete?
    //Not required. Consent is removed via Account App

}


package com.acme.backend.springboot.profileapi.web.consent;

import com.acme.backend.springboot.profileapi.profile.PopulatedUserProfileAttribute;
import lombok.Data;

import java.util.Map;

@Data
public class ConsentFormUpdateRequest {

    String scope;

    Map<String, PopulatedUserProfileAttribute> attributes;
}

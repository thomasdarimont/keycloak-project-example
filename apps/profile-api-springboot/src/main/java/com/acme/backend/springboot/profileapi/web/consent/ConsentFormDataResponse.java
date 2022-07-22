package com.acme.backend.springboot.profileapi.web.consent;

import com.acme.backend.springboot.profileapi.profile.PopulatedUserProfileAttribute;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ConsentFormDataResponse {

    private final Map<String, List<PopulatedUserProfileAttribute>> mapping;
}

package com.acme.backend.springboot.profileapi.web.consent;

import lombok.Data;

@Data
public class ConsentFormDataRequest {

    private String clientId;

    private String scope;
}

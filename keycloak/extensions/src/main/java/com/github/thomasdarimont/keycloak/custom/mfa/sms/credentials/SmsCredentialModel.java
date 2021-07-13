package com.github.thomasdarimont.keycloak.custom.mfa.sms.credentials;

import lombok.extern.jbosslog.JBossLog;
import org.keycloak.credential.CredentialModel;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@JBossLog
public class SmsCredentialModel extends CredentialModel {

    public static final String TYPE = "mfa-sms";

    private String phoneNumber;

    public SmsCredentialModel() {
        this(null);
    }

    public SmsCredentialModel(CredentialModel credentialModel) {
        setType(TYPE);
        if (credentialModel != null) {
            this.setId(credentialModel.getId());
            this.setCreatedDate(credentialModel.getCreatedDate());
            this.setCredentialData(credentialModel.getCredentialData());
            this.setSecretData(credentialModel.getSecretData());
        }
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void writeCredentialData() {
        Map<String, String> credentialData = new HashMap<>();
        credentialData.put(IDToken.PHONE_NUMBER, phoneNumber);

        try {
            setCredentialData(JsonSerialization.writeValueAsString(credentialData));
        } catch (IOException e) {
            log.errorf(e, "Could not serialize SMS credentialData");
        }
    }

    public void readCredentialData() {
        try {
            Map map = JsonSerialization.readValue(getCredentialData(), Map.class);
            setPhoneNumber((String) map.get(IDToken.PHONE_NUMBER));
        } catch (IOException e) {
            log.errorf(e, "Could not deserialize SMS Credential data");
        }
    }
}

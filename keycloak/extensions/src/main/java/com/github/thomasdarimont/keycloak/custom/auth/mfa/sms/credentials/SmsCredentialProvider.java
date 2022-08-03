package com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.credentials;

import com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.PhoneNumberUtils;
import com.github.thomasdarimont.keycloak.custom.auth.mfa.sms.updatephone.UpdatePhoneNumberRequiredAction;
import com.google.auto.service.AutoService;
import lombok.extern.jbosslog.JBossLog;
import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.CredentialProvider;
import org.keycloak.credential.CredentialProviderFactory;
import org.keycloak.credential.CredentialTypeMetadata;
import org.keycloak.credential.CredentialTypeMetadataContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.IDToken;

@JBossLog
public class SmsCredentialProvider implements CredentialProvider<CredentialModel>, CredentialInputValidator {

    public static final String ID = "acme-mfa-sms";

    private final KeycloakSession session;

    public SmsCredentialProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        return SmsCredentialModel.TYPE.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
        return user.credentialManager().getStoredCredentialsByTypeStream(credentialType).findAny().isPresent();
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput credentialInput) {
        return false;
    }

    @Override
    public String getType() {
        return SmsCredentialModel.TYPE;
    }

    @Override
    public CredentialModel createCredential(RealmModel realm, UserModel user, CredentialModel credentialModel) {

        if (!(credentialModel instanceof SmsCredentialModel)) {
            return null;
        }

        SmsCredentialModel model = (SmsCredentialModel) credentialModel;

        String phoneNumber = extractPhoneNumber(model, user);

        model.setType(SmsCredentialModel.TYPE);
        model.setCreatedDate(Time.currentTimeMillis());
        model.setUserLabel("SMS @ " + PhoneNumberUtils.abbreviatePhoneNumber(phoneNumber));
        model.writeCredentialData();

        user.credentialManager().createStoredCredential(model);

        return model;
    }

    private String extractPhoneNumber(SmsCredentialModel model, UserModel user) {
        if (model.getPhoneNumber() != null) {
            return model.getPhoneNumber();
        }

        return user.getFirstAttribute(IDToken.PHONE_NUMBER);
    }

    @Override
    public boolean deleteCredential(RealmModel realm, UserModel user, String credentialId) {
        return user.credentialManager().removeStoredCredentialById(credentialId);
    }

    @Override
    public CredentialModel getCredentialFromModel(CredentialModel model) {

        if (!getType().equals(model.getType())) {
            return null;
        }

        return model;
    }

    @Override
    public CredentialTypeMetadata getCredentialTypeMetadata(CredentialTypeMetadataContext metadataContext) {

        CredentialTypeMetadata.CredentialTypeMetadataBuilder builder = CredentialTypeMetadata.builder();
        builder.type(getType());
        builder.category(CredentialTypeMetadata.Category.TWO_FACTOR);
        builder.createAction(UpdatePhoneNumberRequiredAction.ID);
        builder.removeable(true);
        builder.displayName("mfa-sms-display-name");
        builder.helpText("mfa-sms-help-text");
        // builder.updateAction(GenerateBackupCodeAction.ID);
        // TODO configure proper FA icon for sms auth
        builder.iconCssClass("kcAuthenticatorMfaSmsClass");
        return builder.build(session);
    }

    @AutoService(CredentialProviderFactory.class)
    public static class Factory implements CredentialProviderFactory<SmsCredentialProvider> {

        @Override
        public CredentialProvider<CredentialModel> create(KeycloakSession session) {
            return new SmsCredentialProvider(session);
        }

        @Override
        public String getId() {
            return SmsCredentialProvider.ID;
        }
    }

}

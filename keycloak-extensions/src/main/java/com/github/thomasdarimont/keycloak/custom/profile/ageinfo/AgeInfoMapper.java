package com.github.thomasdarimont.keycloak.custom.profile.ageinfo;

import com.google.auto.service.AutoService;
import com.google.common.annotations.VisibleForTesting;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@AutoService(ProtocolMapper.class)
public class AgeInfoMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    static final String PROVIDER_ID = "oidc-acme-ageinfo-mapper";

    static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

    public static final String AGE_CLASS_CLAIM = "acme_age_class";

    static {

        List<ProviderConfigProperty> configProperties = new ArrayList<>();
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, AgeInfoMapper.class);

        CONFIG_PROPERTIES = configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Acme: AgeInfo";
    }

    @Override
    public String getHelpText() {
        return "Exposes the user's age-class as claim";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return CONFIG_PROPERTIES;
    }

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {

        UserModel user = userSession.getUser();
        String birthdate = user.getFirstAttribute("birthdate");
        String ageClass = computeAgeClass(birthdate);

        token.getOtherClaims().put(AGE_CLASS_CLAIM, ageClass);
    }

    @VisibleForTesting
    String computeAgeClass(String maybeBirthdate) {

        if (maybeBirthdate == null) {
            return "missing";
        }

        LocalDate birthdate = parseLocalDate(maybeBirthdate);
        if (birthdate == null) {
            return "invalid";
        }

        long ageInYears = ChronoUnit.YEARS.between(birthdate, LocalDateTime.now());
        String ageClass = "under16";

        if (ageInYears >= 16) {
            ageClass = "over16";
        }
        if (ageInYears >= 18) {
            ageClass = "over18";
        }
        if (ageInYears >= 21) {
            ageClass = "over21";
        }

        return ageClass;
    }

    private LocalDate parseLocalDate(String maybeLocalDate) {
        try {
            // 1983-01-01
            return LocalDate.parse(maybeLocalDate);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}


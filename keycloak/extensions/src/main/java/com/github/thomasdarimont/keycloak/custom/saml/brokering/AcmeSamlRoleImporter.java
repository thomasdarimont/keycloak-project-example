package com.github.thomasdarimont.keycloak.custom.saml.brokering;

import com.google.auto.service.AutoService;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProviderMapper;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProviderFactory;
import org.keycloak.broker.saml.mappers.AbstractAttributeToRoleMapper;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@AutoService(IdentityProviderMapper.class)
public class AcmeSamlRoleImporter extends AbstractAttributeToRoleMapper {

    public static final String PROVIDER_ID = "acme-saml-dynamic-role-idp-mapper";

    public static final String ROLE_ATTRIBUTE = "roleAttribute";

    public static final String FILTER_PATTERN = "roleNameFilterPattern";

    public static final String EXTRACTION_PATTERN = "roleNameExtractionPattern";

    private static final Set<IdentityProviderSyncMode> IDENTITY_PROVIDER_SYNC_MODES = new HashSet<>(Arrays.asList(IdentityProviderSyncMode.values()));

    public static final String[] COMPATIBLE_PROVIDERS = {SAMLIdentityProviderFactory.PROVIDER_ID};

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(ROLE_ATTRIBUTE);
        property.setLabel("Role Attribute");
        property.setDefaultValue("Roles");
        property.setHelpText("Name of the attributes to search for in SAML assertion.");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(FILTER_PATTERN);
        property.setLabel("Role filter pattern");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue("");
        property.setHelpText("If set, only roles that match the filter pattern are included.");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(EXTRACTION_PATTERN);
        property.setLabel("Role extraction pattern");
        property.setType(ProviderConfigProperty.STRING_TYPE);
        property.setDefaultValue("");
        property.setHelpText("If set, the result of the first group match will be used as rolename");
        configProperties.add(property);
    }

    @Override
    public boolean supportsSyncMode(IdentityProviderSyncMode syncMode) {
        return IDENTITY_PROVIDER_SYNC_MODES.contains(syncMode);
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String[] getCompatibleProviders() {
        return COMPATIBLE_PROVIDERS;
    }

    @Override
    public String getDisplayCategory() {
        return "Role Importer";
    }

    @Override
    public String getDisplayType() {
        return "Acme SAML Role Importer.";
    }

    @Override
    public String getHelpText() {
        return "Maps incoming roles based on a filter pattern to existing roles.";
    }

    @Override
    public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {

        if (!this.applies(mapperModel, context)) {
            return;
        }
        List<RoleModel> roles = getRolesForUser(context, mapperModel);
        roles.forEach(user::grantRole);
    }

    private List<RoleModel> getRolesForUser(BrokeredIdentityContext context, IdentityProviderMapperModel mapperModel) {

        AssertionType assertion = (AssertionType) context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);
        Set<AttributeStatementType> attributeAssertions = assertion.getAttributeStatements();
        if (attributeAssertions == null) {
            return List.of();
        }

        String filterPatternString = mapperModel.getConfig().getOrDefault(FILTER_PATTERN, ".*");
        Pattern filterPattern = Pattern.compile(filterPatternString);

        String extractionPatternString = mapperModel.getConfig().getOrDefault(EXTRACTION_PATTERN, ".*");
        Pattern extractionPattern = Pattern.compile(extractionPatternString);

        String roleAttribute = mapperModel.getConfig().getOrDefault(ROLE_ATTRIBUTE, "Role");

        RealmModel realm = context.getAuthenticationSession().getRealm();
        List<RoleModel> roles = new ArrayList<>();
        for (var attributeStatement : attributeAssertions) {
            for (var attr : attributeStatement.getAttributes()) {
                var attribute = attr.getAttribute();

                if (!roleAttribute.equals(attribute.getName())) {
                    continue;
                }

                for (var value : attribute.getAttributeValue()) {
                    if (value == null) {
                        continue;
                    }

                    String roleName = value.toString();
                    if (roleName.isBlank()) {
                        continue;
                    }

                    if (!filterPattern.matcher(roleName).matches()) {
                        continue;
                    }

                    Matcher matcher = extractionPattern.matcher(roleName);
                    if (!matcher.matches()) {
                        continue;
                    }

                    var extractedRoleName = matcher.group(1);
                    RoleModel role = KeycloakModelUtils.getRoleFromString(realm, extractedRoleName);
                    if (role == null) {
                        continue;
                    }

                    roles.add(role);
                }
            }
        }

        return roles;
    }

    @Override
    public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user, IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {

        if (!this.applies(mapperModel, context)) {
            return;
        }

        List<RoleModel> roles = getRolesForUser(context, mapperModel);
        roles.forEach(user::grantRole);

//        RoleModel role = getRole(realm, mapperModel);
//        String roleName = mapperModel.getConfig().get(ConfigConstants.ROLE);
//        // KEYCLOAK-8730 if a previous mapper has already granted the same role, skip the checks so we don't accidentally remove a valid role.
//        if (!context.hasMapperGrantedRole(roleName)) {
//            if (this.applies(mapperModel, context)) {
//                context.addMapperGrantedRole(roleName);
//                user.grantRole(role);
//            } else {
//                user.deleteRoleMapping(role);
//            }
//        }
    }

    protected boolean applies(final IdentityProviderMapperModel mapperModel, final BrokeredIdentityContext context) {
        AssertionType assertion = (AssertionType) context.getContextData().get(SAMLEndpoint.SAML_ASSERTION);
        Set<AttributeStatementType> attributeAssertions = assertion.getAttributeStatements();
        return attributeAssertions != null;
    }
}

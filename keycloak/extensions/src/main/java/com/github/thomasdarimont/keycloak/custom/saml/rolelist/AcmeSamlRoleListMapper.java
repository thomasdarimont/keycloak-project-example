package com.github.thomasdarimont.keycloak.custom.saml.rolelist;

import com.google.auto.service.AutoService;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AbstractSAMLProtocolMapper;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.SAMLAttributeStatementMapper;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AutoService(ProtocolMapper.class)
public class AcmeSamlRoleListMapper extends AbstractSAMLProtocolMapper implements SAMLAttributeStatementMapper {

    public static final String PROVIDER_ID = "acme-saml-role-list-mapper";

    public static final String SINGLE_ROLE_ATTRIBUTE = "single";

    public static final String PREFIX_CLIENT_ROLES = "prefixClientRoles";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<>();

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(AttributeStatementHelper.SAML_ATTRIBUTE_NAME);
        property.setLabel("Role attribute name");
        property.setDefaultValue("Role");
        property.setHelpText("Name of the SAML attribute you want to put your roles into.  i.e. 'Role', 'memberOf'.");
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(AttributeStatementHelper.FRIENDLY_NAME);
        property.setLabel(AttributeStatementHelper.FRIENDLY_NAME_LABEL);
        property.setHelpText(AttributeStatementHelper.FRIENDLY_NAME_HELP_TEXT);
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT);
        property.setLabel("SAML Attribute NameFormat");
        property.setHelpText("SAML Attribute NameFormat.  Can be basic, URI reference, or unspecified.");
        property.setType(ProviderConfigProperty.LIST_TYPE);
        property.setOptions(List.of(AttributeStatementHelper.BASIC, //
                AttributeStatementHelper.URI_REFERENCE, //
                AttributeStatementHelper.UNSPECIFIED));
        configProperties.add(property);
        property = new ProviderConfigProperty();
        property.setName(SINGLE_ROLE_ATTRIBUTE);
        property.setLabel("Single Role Attribute");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue("true");
        property.setHelpText("If true, all roles will be stored under one attribute with multiple attribute values.");
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(PREFIX_CLIENT_ROLES);
        property.setLabel("Prefix client roles with clientId");
        property.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property.setDefaultValue("true");
        property.setHelpText("If true, all client roles will be prefixed with the clientId followed by ':'");
        configProperties.add(property);
    }

    @Override
    public String getDisplayCategory() {
        return "Role Mapper";
    }

    @Override
    public String getDisplayType() {
        return "Acme SAML Role list";
    }

    @Override
    public String getHelpText() {
        return "Role names are stored in an attribute value.  There is either one attribute with multiple attribute values, or an attribute per role name depending on how you configure it.  You can also specify the attribute name i.e. 'Role' or 'memberOf' being examples.";
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
    public void transformAttributeStatement(AttributeStatementType attributeStatement, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, AuthenticatedClientSessionModel clientSession) {

        var singleAttribute = Boolean.parseBoolean(mappingModel.getConfig().get(SINGLE_ROLE_ATTRIBUTE));
        var prefixClientRoles = Boolean.parseBoolean(mappingModel.getConfig().get(PREFIX_CLIENT_ROLES));

        AttributeType singleAttributeType = null;

        var allRoles = RoleUtils.expandCompositeRoles(userSession.getUser().getRoleMappingsStream().collect(Collectors.toSet()));

        for (RoleModel role : allRoles) {
            AttributeType attributeType;
            if (singleAttribute) {
                if (singleAttributeType == null) {
                    singleAttributeType = AttributeStatementHelper.createAttributeType(mappingModel);
                    attributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(singleAttributeType));
                }
                attributeType = singleAttributeType;
            } else {
                attributeType = AttributeStatementHelper.createAttributeType(mappingModel);
                attributeStatement.addAttribute(new AttributeStatementType.ASTChoiceType(attributeType));
            }

            var roleName = role.getName();

            if (prefixClientRoles && role.isClientRole()) {
                roleName = ((ClientModel)role.getContainer()).getClientId() + ":" + roleName;
            }

            attributeType.addAttributeValue(roleName);
        }

    }

    public static ProtocolMapperModel create(String name, String samlAttributeName, String nameFormat, String friendlyName, boolean singleAttribute, boolean prefixClientRoles) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(SamlProtocol.LOGIN_PROTOCOL);
        Map<String, String> config = new HashMap<>();
        config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAME, samlAttributeName);
        if (friendlyName != null) {
            config.put(AttributeStatementHelper.FRIENDLY_NAME, friendlyName);
        }
        if (nameFormat != null) {
            config.put(AttributeStatementHelper.SAML_ATTRIBUTE_NAMEFORMAT, nameFormat);
        }
        config.put(SINGLE_ROLE_ATTRIBUTE, Boolean.toString(singleAttribute));
        config.put(PREFIX_CLIENT_ROLES, Boolean.toString(prefixClientRoles));
        mapper.setConfig(config);

        return mapper;
    }

}


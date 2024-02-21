package keycloak.utils.kc

import future.keywords.if
import future.keywords.in

isRealm(realmName) if input.resource.realm == realmName

isClient(clientId) if input.resource.clientId == clientId

hasRealmRole(roleName) if roleName in input.subject.realmRoles

hasClientRole(clientId, roleName) := result if {
	client_role := concat(":", [clientId, roleName])
	result := client_role in input.subject.clientRoles
}

hasCurrentClientRole(roleName) := result if {
	client_role := concat(":", [input.resource.clientId, roleName])
	result := client_role in input.subject.clientRoles
}

hasUserAttribute(attribute) if input.subject.attributes[attribute]

hasUserAttributeValue(attribute, value) if input.subject.attributes[attribute] == value

isGroupMember(group) if group in input.subject.groups

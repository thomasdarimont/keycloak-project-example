package keycloak.realms.opademo2.access

import future.keywords.if
import future.keywords.in

import data.keycloak.utils.kc

# default rule "allow"
default allow := false

# rule "allow" for client-id:account-console
allow if {
	kc.isClient("account-console")
}
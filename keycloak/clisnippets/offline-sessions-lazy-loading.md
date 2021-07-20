
embed-server --server-config=standalone-ha.xml --std-out=echo

/subsystem=logging/console-handler=CONSOLE:write-attribute(name=level,value=DEBUG)

if (outcome == failed) of /subsystem=logging/logger=org.keycloak.models.sessions.infinispan/:read-resource
/subsystem=logging/logger=org.keycloak.models.sessions.infinispan:add(level=DEBUG)
end-if

###### 

echo SETUP: Customize Keycloak UserSessions SPI configuration
# Add dedicated userSession config element to allow configuring elements.
if (outcome == failed) of /subsystem=keycloak-server/spi=userSessions/:read-resource
echo SETUP: Add missing userSessions SPI
/subsystem=keycloak-server/spi=userSessions:add()
echo
end-if

echo SETUP: Configure built-in "infinispan"  UserSessions loader
if (outcome == failed) of /subsystem=keycloak-server/spi=userSessions/provider=infinispan/:read-resource
echo SETUP: Add missing "infinispan" provider
/subsystem=keycloak-server/spi=userSessions/provider=infinispan:add(enabled=true)
/subsystem=keycloak-server/spi=userSessions/provider=infinispan:write-attribute(name=properties.preloadOfflineSessionsFromDatabase,value=${env.KEYCLOAK_INFINISPAN_SESSIONS_PRELOAD_DATABASE:false})
/subsystem=keycloak-server/spi=userSessions:write-attribute(name=default-provider,value=infinispan)
echo
end-if
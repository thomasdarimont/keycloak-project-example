Undertow Configuration
----

```
### Undertow Configuration ###

echo SETUP: Adjust Undertow configuration

## See undertow configuration
# https://access.redhat.com/documentation/en-us/red_hat_jboss_enterprise_application_platform/7.4-beta/html/configuration_guide/configuring_the_web_server_undertow#undertow-configure-filters
# https://undertow.io/undertow-docs/undertow-docs-2.1.0/index.html#predicates-attributes-and-handlers

# Reject requests for clients-registrations and the welcome page
# -> response-code(302)
# -> redirect('https://example.com')
# -> redirect('${env.KEYCLOAK_FRONTEND_URL}')
/subsystem=undertow/configuration=filter/expression-filter=rejectAccessDefault:add( \
expression="(regex('/auth/realms/.*/clients-registrations/openid-connect') or path('/auth/'))-> response-code(403)" \
)
/subsystem=undertow/server=default-server/host=default-host/filter-ref=rejectAccessDefault:add()
```
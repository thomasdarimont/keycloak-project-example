Example for mapping a Keycloak endpoint to a custom endpoint to workaround bugs
---

```
# Map Keycloak paths to custom endpoints to work around Keycloak bugs (the UUID regex matches a UUID V4 (random uuid pattern)
/subsystem=undertow/configuration=filter/expression-filter=keycloakPathOverrideConsentEndpoint:add( \
  expression="regex('/auth/admin/realms/acme-internal/users/([a-f\\d]{8}-[a-f\\d]{4}-4[a-f\\d]{3}-[89ab][a-f\\d]{3}-[a-f\\d]{12})/consents') -> rewrite('/auth/realms/acme-internal/custom-resources/users/$1/consents')" \
)
/subsystem=undertow/server=default-server/host=default-host/filter-ref=keycloakPathOverrideConsentEndpoint:add()
```
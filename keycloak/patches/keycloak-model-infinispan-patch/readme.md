Keycloak Model Infinispan Patch
----

Patched version of the `keycloak-model-infinispan` library that allows to use dedicated
cache store configuration with the embedded infinispan support.

As of Keycloak version 15.0.2, it is not supported to use custom cache store configurations with Keycloak, 
as Keycloak skips writes to configured cache stores by default. See the usage of
`org.keycloak.models.sessions.infinispan.CacheDecorators#skipCacheStore`.

To work around this, we patch `org.keycloak.models.sessions.infinispan.CacheDecorators` to consider the 
new system property `keycloak.infinispan.ignoreSkipCacheStore` to control whether it is possible to
propagate a cache write to a configured cache store. Setting `-Dkeycloak.infinispan.ignoreSkipCacheStore=true`
allows to propagate cache writes to configured cache store to backends like jbdc-datasource, redis etc.

An example configuration with this patch can be found in `deployments/local/cluster/haproxy-database-ispn`. 
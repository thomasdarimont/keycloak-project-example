Keycloak with Database backed Sessions
---
# Generate keys

To generate the certificate and keys install https://github.com/FiloSottile/mkcert and run the following command:
```
mkcert -install
mkcert "*.acme.test"
```

# Additional libraries

In order to apply the cache store configuration the following libraries are needed that are currently not packaged with Keycloak:
- infinispan-cachestore-jdbc-13.0.10.Final.jar
- infinispan-cachestore-jdbc-common-13.0.10.Final.jar

# Required patches

## keycloak-model-infinispan-20.0.1.jar

* "Replace operation set wrong lifespan in remote infinispan database an… #15619"  
backported to 20.0.1  

This fixes the computation of the cache item timestamp for remote stores.

See: https://github.com/keycloak/keycloak/pull/15619#issuecomment-1324187372

* Changed CacheDecorators to support to ignore skipCacheStore hints  
backported to 20.0.1  

This is necessary in order to propagate the cache write to the configured persistance store.  
This behaviour can be activated with the system property `-Dkeycloak.infinispan.ignoreSkipCacheStore=true`

See: https://github.com/thomasdarimont/keycloak-project-example/blob/main/keycloak/patches/keycloak-model-infinispan-patch/src/main/java/org/keycloak/models/sessions/infinispan/CacheDecorators.java#L25
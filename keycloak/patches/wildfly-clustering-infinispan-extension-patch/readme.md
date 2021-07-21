Patched Wildfly Clustering Infinispan Extension
---

The current wildfly version 23.0.2 used by Keycloak 14.0.0 does not support the configuration of a `connect-timeout` for infinispan remote cache stores.

This repo contains a patched version of [Wildflys Infinispan Extension](https://github.com/wildfly/wildfly/tree/master/clustering/infinispan/extension)
with proper support for configuring `connect-timeouts`.

See the related wildfly issue: [https://issues.redhat.com/browse/WFLY-15046](https://issues.redhat.com/browse/WFLY-15046).

A docker volume mount for the patch could look like this:
```
./patch/wildfly-clustering-infinispan-extension-patch.jar:/opt/jboss/keycloak/modules/system/layers/base/org/jboss/as/clustering/infinispan/main/wildfly-clustering-infinispan-extension-23.0.2.Final.jar:z
```

An usage example can be found in [haproxy-external-ispn](/deployments/local/cluster/haproxy-external-ispn).
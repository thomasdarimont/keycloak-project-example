Keycloak Clustering Examples
----

# Cluster with haproxy Load-Balancer 

## Prepare

Copy the `acme.test*.pem` files from the `config/stage/dev/tls` into the [haproxy](haproxy) directory.

## Run 
```
docker-compose --env-file ../../../keycloak.env --file haproxy/docker-compose-haproxy.yml up --remove-orphans
```

haproxy status URL: https://id.acme.test:1443/haproxy?status

HAProxy Keycloak URL: https://id.acme.test:1443/auth

## Run with encrypted and authenticated JGroups traffic

The encryption uses JGroup's `SYM_ENCRYPT` protocol with AES encryption by default.
Note that you might generate a new PKCS12 keystore with a secretkey via the script in `haproxy-encrypted-ispn/jgroups-keystore.sh`.
Make sure that every Keycloak instance in the cluster must use the exactly same file.

The JGroups authentication uses the `AUTH` module with a pre-shared key. 

```
docker-compose --env-file ../../../keycloak.env --file haproxy-encrypted-ispn/docker-compose-enc-haproxy.yml up --remove-orphans
```

## Run with Infinispan cache content stored in jdbc-store

This example shows how to store data from the user session cache in a database that survives restarts.

```
docker-compose --env-file ../../../keycloak.env --file haproxy-database-ispn/docker-compose-haproxy-jdbc-store.yml up --remove-orphans
```

## Run with dedicated Infinispan Cluster with Remote store

The haproxy example can also be started with a dedicated infinispan cluster where the 
distributed and replicated caches in Keycloak will be stored in an external infinispan cluster with cache store type `remote`. 

Note that this example uses a [patched version](../../../keycloak/patches/wildfly-clustering-infinispan-extension-patch) of the `wildfly-clustering-infinispan-extension.jar` in order to
allow to configure a `connect-timeout` on the remote-store.

To start the environment with a dedicated infinispan cluster, just run:
```
docker-compose --env-file ../../../keycloak.env --file haproxy-external-ispn/docker-compose-haproxy-ispn-remote.yml up
```

## Run with dedicated Infinispan Cluster with Hotrod store

[This doesn't work at the moment](https://github.com/thomasdarimont/keycloak-project-example/issues/22) try to use the [Infinispan Cluster with Remote store](##Run with dedicated Infinispan Cluster with Remote store) variant.

The haproxy example can also be started with a dedicated infinispan cluster where the
distributed and replicated caches in Keycloak will be stored in an external infinispan cluster with cache store type `hotrod`

To start the environment with a dedicated infinispan cluster, just run:
```
docker-compose --env-file ../../../keycloak.env --file haproxy-external-ispn/docker-compose-haproxy-ispn-hotrod.yml up
```

# Cluster with nginx Load-Balancer

## Run
```
docker-compose --env-file ../../../keycloak.env --file nginx/docker-compose-nginx.yml up --remove-orphans
```

Nginx Keycloak URL: https://id.acme.test:2443/auth

# Cluster with Apache mod_proxy Load-Balancer

## Run
```
docker-compose --env-file ../../../keycloak.env --file apache/docker-compose-apache.yml up --remove-orphans
```

Apache Keycloak URL: https://id.acme.test:3443/auth

# Cluster with Caddy Load-Balancer

## Run
```
docker-compose --env-file ../../../keycloak.env --file caddy/docker-compose-caddy.yml up --remove-orphans
```

Caddy Keycloak URL: https://id.acme.test:5443/auth

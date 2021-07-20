Clustered Keycloak with Remote Infinispan Cache configuration behind haproxy
---

# Prepare Infinispan Keystore and Truststore

```
keytool -genkey \
  -alias server \
  -keyalg RSA \
  -keystore ispn-server.jks \
  -keysize 2048 \
  -storepass password \
  -dname "CN=ispn, OU=keycloak, O=tdlabs, L=Saarbrücken, ST=SL, C=DE"

keytool -exportcert \
  -keystore ispn-server.jks \
  -alias server \
  -storepass password \
  -file ispn-server.crt

keytool -importcert \
  -keystore ispn-truststore.jks \
  -storepass password \
  -alias server \
  -file ispn-server.crt \
  -noprompt

rm ispn-server.crt
```

# Patch CA Certs

As of Keycloak image 14.0.0 the used JDK Truststore contains expired certificates which lead to an 
exception during server start. To fix this, we need to remove the expired certificates.

Copy the cacerts keystore from a running Keycloak container locally
```
docker cp gifted_bhaskara:/etc/pki/ca-trust/extracted/java/cacerts ./ispn/cacerts
chmod u+w cacerts
```

```
keytool -delete -keystore ./ispn/cacerts -alias quovadisrootca -storepass changeit
keytool -delete -keystore ./ispn/cacerts -alias soneraclass2rootca -storepass changeit

chmod u-w ./ispn/cacerts
```

Now mount the fixed `cacerts` into the container via `./ispn/cacerts:/etc/pki/ca-trust/extracted/java/cacerts:z`


# Start Environment

```
cd ..
docker-compose --env-file ../../../keycloak.env --file haproxy-external-ispn/docker-compose-haproxy-ispn.yml  up --remove-orphans
```

## Problems

- Cannot configure connect-timeout for remote caches as the configuration attribute is not supported by wildfly.
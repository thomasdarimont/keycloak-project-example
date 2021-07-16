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

# Start Environment

```
cd ..
docker-compose --env-file ../../../keycloak.env --file haproxy-external-ispn/docker-compose-haproxy-ispn.yml  up --remove-orphans
```
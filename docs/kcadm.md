Keycloak Admin Client
---

The Keycloak distribution ships with the `kcadm.sh` CLI tool that allows to manage Keycloak realm configurations.

# kcadm setup

Although it is possible to use a `kcadm.sh` from a local Keycloak installation, we recommend to use the `kcadm.sh` that is provided
from the Keycloak docker image, to ensure that compatible versions are used.

To use `kcadm.sh` from the Keycloak docker image, we define the following `kcadm` alias: 
```
alias kcadm="docker run --net=host -i --user=1000:1000 --rm -v $(echo $HOME)/.acme/.keycloak:/opt/jboss/.keycloak:z --entrypoint /opt/jboss/keycloak/bin/kcadm.sh quay.io/keycloak/keycloak:13.0.1"
```

# Generate kcadm Truststore
```
keytool  \
-import  \
-file "config/stage/dev/tls/acme.test+1.pem" \
-keystore run/kcadm-truststore.jks \
-alias keycloak \
-storepass changeit \
-noprompt
```

# Configure kcadm
```
KEYCLOAK_REALM=acme-internal
TRUSTSTORE_PASSWORD=changeit
KEYCLOAK_URL=https://id.acme.test:8443/auth
KEYCLOAK_ADMIN_USER=admin
KEYCLOAK_ADMIN_PASSWORD=admin

kcadm config truststore --storepass $TRUSTSTORE_PASSWORD /opt/jboss/.keycloak/kcadm-truststore.jks
kcadm config credentials --server $KEYCLOAK_URL --realm master --user $KEYCLOAK_ADMIN_USER --password $KEYCLOAK_ADMIN_PASSWORD --trustpass $TRUSTSTORE_PASSWORD
```

# Use kcadm
```
kcadm get clients -r $KEYCLOAK_REALM --fields="id,clientId" --trustpass $TRUSTSTORE_PASSWORD
kcadm get users -r $KEYCLOAK_REALM --fields="id,username,email" --trustpass $TRUSTSTORE_PASSWORD
kcadm get users -r $KEYCLOAK_REALM --trustpass $TRUSTSTORE_PASSWORD
kcadm create users -r $KEYCLOAK_REALM -s username=demo -s firstName=Doris -s lastName=Demo -s email='doris@localhost' -s enabled=true  --trustpass $TRUSTSTORE_PASSWORD
```

# Misc

If you don't have the certificate you could try to download it from the server.
```
echo -n | openssl s_client -connect id.acme.test:8443 -servername id.acme.test \
    | openssl x509 > /tmp/id.acme.test.cert
```
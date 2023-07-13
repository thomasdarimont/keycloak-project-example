Keycloak Admin Client (kcadm)
---

The Keycloak distribution ships with [kcadm.sh CLI tool](https://github.com/keycloak/keycloak-documentation/blob/master/server_admin/topics/admin-cli.adoc) that allows to manage Keycloak realm configurations.

# kcadm setup
Although it is possible to use a `kcadm.sh` from a local Keycloak installation, we recommend to use the `kcadm.sh` that is provided from the Keycloak docker image, to ensure that compatible versions are used.

## Setup command
To use `kcadm.sh` from the Keycloak docker image, we define the alias `kcadm`: 
```
alias kcadm="docker run --net=host -i --user=1000:1000 --rm -v $(echo $HOME)/.acme/.keycloak:/opt/keycloak/.keycloak:z --entrypoint /opt/keycloak/bin/kcadm.sh quay.io/keycloak/keycloak:22.0.0"
```
## Setup environment 
variables for clean commands
```
KEYCLOAK_REALM=acme-internal
TRUSTSTORE_PASSWORD=changeit
KEYCLOAK_URL=https://id.acme.test:8443/auth
KEYCLOAK_ADMIN=admin
KEYCLOAK_ADMIN_PASSWORD=admin
KEYCLOAK_CLIENT=demo-client
```
## Usage with http or https
When keycloak is started locally with `--http` (default) nothing is encrypted and no truststore is used.
The following three subsections can be skipped.
Additionally, all the commands do not need the `--trustpass $TRUSTSTORE_PASSWORD` part.

When using `--https` all traffic is encrypted, and the truststore is required.
Please follow the steps to make the truststore available and pass it for all commands.

### Download server certificate (if necessary) 
```
echo -n | openssl s_client -connect id.acme.test:8443 -servername id.acme.test \
    | openssl x509 > /tmp/id.acme.test.cert
```
### Generate kcadm Truststore
```
keytool  \
-import  \
-file "config/stage/dev/tls/acme.test+1.pem" \
-keystore $(echo $HOME)/.acme/.keycloak/kcadm-truststore.jks \
-alias keycloak \
-storepass $TRUSTSTORE_PASSWORD \
-noprompt
``` 

### Configure Truststore with kcadm

```
kcadm config truststore --storepass $TRUSTSTORE_PASSWORD /opt/keycloak/.keycloak/kcadm-truststore.jks
```

## Configure credentials
```
kcadm config credentials --server $KEYCLOAK_URL --realm master --user $KEYCLOAK_ADMIN --password $KEYCLOAK_ADMIN_PASSWORD --trustpass $TRUSTSTORE_PASSWORD
```

# Use cases
We collect a list of useful commands here.
More examples can be found in the [official documentation](https://github.com/keycloak/keycloak-documentation/blob/master/server_admin/topics/admin-cli.adoc).

## Get realms
```
kcadm get realms --fields="id,realm" --trustpass $TRUSTSTORE_PASSWORD

kcadm get realms/$KEYCLOAK_REALM --trustpass $TRUSTSTORE_PASSWORD

```

## Create realms
```
kcadm create realms -s realm=$KEYCLOAK_REALM -s enabled=true
```

## Update realms
```
kcadm update realms/$KEYCLOAK_REALM -s "enabled=false" --trustpass $TRUSTSTORE_PASSWORD

kcadm update realms/$KEYCLOAK_REALM -s "displayNameHtml=Wonderful world" --trustpass $TRUSTSTORE_PASSWORD
```

## Get clients
```
kcadm get clients -r $KEYCLOAK_REALM --fields="id,clientId" --trustpass $TRUSTSTORE_PASSWORD
```
## Create clients
```
kcadm create clients -r $KEYCLOAK_REALM  --trustpass $TRUSTSTORE_PASSWORD  -f - << EOF
  {
    "clientId": "demo-client",
    "rootUrl": "http://localhost:8090",
    "baseUrl": "/",
    "surrogateAuthRequired": false,
    "enabled": true,
    "alwaysDisplayInConsole": false,
    "clientAuthenticatorType": "client-secret",
    "secret": "1f88bd14-7e7f-45e7-be27-d680da6e48d8",
    "redirectUris": ["/*"],
    "webOrigins": ["+"],
    "bearerOnly": false,
    "consentRequired": false,
    "standardFlowEnabled": true,
    "implicitFlowEnabled": false,
    "directAccessGrantsEnabled": false,
    "serviceAccountsEnabled": false,
    "publicClient": false,
    "frontchannelLogout": false,
    "protocol": "openid-connect",
    "defaultClientScopes": ["web-origins","role_list","roles","profile","email"],
    "optionalClientScopes": ["address","phone","offline_access","microprofile-jwt"]
  }
EOF
```

## Update clients (e.g. secret) 
Find id of client...
```
clientUuid=$(kcadm get clients -r $KEYCLOAK_REALM  --fields 'id,clientId' --trustpass $TRUSTSTORE_PASSWORD | jq -c '.[] | select(.clientId == "'$KEYCLOAK_CLIENT'")' | jq -r .id)
```
...update attributes by id (e.g. client secret)
```
kcadm update clients/$clientUuid -r $KEYCLOAK_REALM -s "secret=abc1234" --trustpass $TRUSTSTORE_PASSWORD

kcadm update clients/$clientUuid -r $KEYCLOAK_REALM -s "publicClient=true" --trustpass $TRUSTSTORE_PASSWORD
```

## Get client by id
```
kcadm get clients/$clientUuid -r $KEYCLOAK_REALM --trustpass $TRUSTSTORE_PASSWORD

kcadm get clients/$clientUuid/client-secret -r $KEYCLOAK_REALM --trustpass $TRUSTSTORE_PASSWORD

```

## Get users
```
kcadm get users -r $KEYCLOAK_REALM --trustpass $TRUSTSTORE_PASSWORD

kcadm get users -r $KEYCLOAK_REALM --fields="id,username,email" --trustpass $TRUSTSTORE_PASSWORD
```

## Create users
```
kcadm create users -r $KEYCLOAK_REALM -s username=demo -s firstName=Doris -s lastName=Demo -s email='doris@localhost' -s enabled=true  --trustpass $TRUSTSTORE_PASSWORD

kcadm create users -r $KEYCLOAK_REALM -s username=tester -s firstName=Theo -s lastName=Tester -s email='tom+tester@localhost' -s enabled=true --trustpass $TRUSTSTORE_PASSWORD

kcadm create users -r $KEYCLOAK_REALM -s username=vadmin -s firstName=Vlad -s lastName=Admin -s email='tom+vlad@localhost' -s enabled=true --trustpass $TRUSTSTORE_PASSWORD
```

## Update users

Find id of client...
```
userUuid=$(kcadm get users -r $KEYCLOAK_REALM  --fields 'id,username' --trustpass $TRUSTSTORE_PASSWORD | jq -c '.[] | select(.username == "'demo'")' | jq -r .id)
```
...update attributes by id (e.g. username)
```
kcadm update users/$userUuid -r $KEYCLOAK_REALM -s "firstName=Dolores" --trustpass $TRUSTSTORE_PASSWORD

kcadm update users/$userUuid -r $KEYCLOAK_REALM -s "enabled=false" --trustpass $TRUSTSTORE_PASSWORD
```
## Set user password
```
kcadm set-password -r $KEYCLOAK_REALM --username tester --new-password test --trustpass $TRUSTSTORE_PASSWORD

kcadm set-password -r $KEYCLOAK_REALM --username vadmin --new-password test --trustpass $TRUSTSTORE_PASSWORD
```

## Get user by id
```
kcadm get users/$userUuid -r $KEYCLOAK_REALM --trustpass $TRUSTSTORE_PASSWORD
```

## Get roles
```
kcadm get roles -r $KEYCLOAK_REALM --trustpass $TRUSTSTORE_PASSWORD
```

## Create roles
```
kcadm create roles -r $KEYCLOAK_REALM -s name=user -o --trustpass $TRUSTSTORE_PASSWORD

kcadm create roles -r $KEYCLOAK_REALM -s name=admin -o --trustpass $TRUSTSTORE_PASSWORD
```

## Assign role to user
```
kcadm add-roles -r $KEYCLOAK_REALM --uusername tester --rolename user --trustpass $TRUSTSTORE_PASSWORD

kcadm add-roles -r $KEYCLOAK_REALM --uusername vadmin --rolename user --rolename admin --trustpass $TRUSTSTORE_PASSWORD
```

## Partial export

```
kcadm create realms/$KEYCLOAK_REALM/partial-export -s exportGroupsAndRoles=true -s exportClients=true -o  --trustpass $TRUSTSTORE_PASSWORD
```

## Export profile

```
kcadm get realms/workshop/users/profile -o --trustpass $TRUSTSTORE_PASSWORD
```
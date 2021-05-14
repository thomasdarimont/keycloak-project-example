Keycloak Project Template
---

This repository contains an easy to use project setup for custom Keycloak projects.

This setup serves as a starting point to develop and deploy a set of Keycloak extensions, custom themens and configuration to a Keycloak docker container.
In addition to that, the project also shows how to write integration tests via [Keycloak-Testcontainers](https://github.com/dasniko/testcontainers-keycloak) and how to package all extensions and themes as a
custom docker image.

The example contains the following Keycloak extensions:
- Custom REST Endpoint the can expose additional custom APIs: `CustomResource`

# Some Highlights
- Support for deploying extensions to running Keycloak container
- Support for instant reloading of theme and extension code changes
- Support Keycloak configuration customization via CLI scripts
- Examples for Integration Tests with Keycloak-Testcontainers
- Example for End to End Tests with Cypress
- Realm configuration as Configuration as Code via keycloak-config-cli scripts 
- Multi-realm setup example with OpenID Connect and SAML based Identity Brokering
- LDAP based User Federation

# Build
The example can be build with the following maven command:
```
mvn clean verify
```

## Build with Integration Tests
The example can be build with integration tests by running the following maven command:
```
mvn clean verify -Pwith-integration-tests
```

## Build Docker Image
To build a custom Keycloak Docker image that contains the custom extensions and themes, you can run the following command:
```
mvn clean verify -Pwith-integration-tests io.fabric8:docker-maven-plugin:build
```

# Run

## Start Keycloak Container with docker-compose

To speed up development we can mount the keycloak-extensions class-folder and keycloak-themes folder into
a Keycloak container that is started via docker-compose. This allows for quick turnarounds while working on themes
and extensions.

Keycloak will be available on http://localhost:8080/auth.
The default Keycloak admin username is `admin` with password `admin`.

You can start the Keycloak container via:
```
mkdir -p testrun/data

docker-compose --env-file custom-keycloak.env up --remove-orphans
```

Note that after changing extensions code you need to run the `bin/triggerDockerExtensionDeploy.sh` script to trigger
a redeployment of the custom extension by Keycloak.

## Running the custom Docker Image

The custom docker image created during the build can be stared with the following command:
```
docker run \
--name custom-keycloak \
-e KEYCLOAK_USER=admin \
-e KEYCLOAK_PASSWORD=admin \
-e KEYCLOAK_CONFIG_FILE=standalone.xml \
-e KEYCLOAK_IMPORT=/opt/jboss/imex/custom-realm.json \
-v $PWD/imex:/opt/jboss/imex:z \
-it \
--rm \
-p 8080:8080 \
thomasdarimont/custom-keycloak:latest
```

# Example environment

## Realms

The example environment contains several realms to illustrate the interaction of different realms.

### Acme-Apps Realm

The `acme-apps` realm contains a simple demo application and provides integration with the `acme-internal`
and `acme-saml` realm via Identity Brokering. The idea behind this setup is to provide a global
`acme-apps` realm for applications that are shared between internal and external users.

The `acme-internal` realm provides applications that are only intended for internal users and employees.
The `acme-internal` realm serves as an OpenID Connect based Identity Provider for the `acme-apps` realm.
The `acme-saml` realm provides applications is similar to the `acme-internal` and serves as 
a SAML based Identity Provider for the `acme-apps` realm.

### Acme-Internal Realm

The `acme-internal` realm contains a test user and is connected to a federated user store (LDAP directory) provided via openldap.

Users:
- Username `tester` and password `test` (from database)
- Username `FleugelR` and password `Password1` (from LDAP federation)

### Acme-SAML Realm

The `acme-saml` realm contains a test user and stores the users in the Keycloak database.

Users:
- Username `acmesaml` and password `test` (from database)

### Example App

A simple demo app can be used to show information from the Access-Token, ID-Token and UserInfo endpoint provided by Keycloak.

The demo app can be started by running `etc/runDemoApp.sh` and will be accessible via http://localhost:4000.

# Scripts

## Manually Trigger Extension Deployment

To manually trigger an extension redeployment after extension code changes / build, you can run the following script:
```
bin/deployExtensions.sh
```

## Exporting a Realm

To export an existing realm as JSON start the docker-compose infrastructure and run the following script.
The export will create a file like `acme-apps-realm.json` in the `./imex` folder.

```
bin/exportRealm.sh acme-apps
```

# Tools

## Mailhog

Web Interface: http://localhost:1080/#
Web API: http://localhost:1080/api/v2/messages

## phpldapadmin

Web Interface: http://localhost:17080
Username: cn=admin,dc=corp,dc=acme,dc=local
Password: admin

# Misc

## Add external tool in IntelliJ to trigger extension deployment

Instead of running the deployment trigger script yourself, you can register it as an external tool in IntelliJ as shown below.

- Name: `kc-deploy-extensions`
- Description: `Deploy Extensions to Keycloak Docker Container`
- Program: `bash`
- Arguments: `$ProjectFileDir$/bin/deployExtensions.sh`
- Working directory: `$ProjectFileDir$`
- Only select: `Synchronize files after execution.`

The extensions can now be redeployed by running `Tools -> External Tools -> kc-deploy-extensions`

## Add external tool in IntelliJ to trigger realm configuration

Instead of running the Keycloak Config CLI script yourself, you can register it as an external tool in IntelliJ as shown below.

- Name: `kc-deploy-config`
- Description: `Deploy Realm Config to Keycloak Docker Container`
- Program: `bash`
- Arguments: `$ProjectFileDir$/bin/runKeycloakConfigCli.sh`
- Working directory: `$ProjectFileDir$`
- Only select: `Synchronize files after execution.`

The extensions can now be redeployed by running `Tools -> External Tools -> kc-deploy-config`

## Scan Image for Vulnerabilities

```
docker run \
  --privileged \
  --rm \
  -v /var/run/docker.sock:/var/run/docker.sock:z aquasec/trivy:0.17.2 \
  thomasdarimont/custom-keycloak:1.0.0-SNAPSHOT
```
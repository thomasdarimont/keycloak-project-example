Keycloak Project Example
---
# Introduction
This repository contains a project setup for keycloak based projects.

This setup serves as a starting point to support the full lifecycle of development in a keycloak based project. 
This may include develop and deploy a set of Keycloak extensions, custom themes and configuration into a customized keycloak docker container (or tar-ball).

The project also shows how to write integration tests via [Keycloak-Testcontainers](https://github.com/dasniko/testcontainers-keycloak).
After successful test-run package all extensions and themes as a custom docker image.
This image is meant to be the project base image fulfilling the projects requirements in contrast to the general keycloak image.

## Use-Cases
These requirements work in different contextes, roles and use-cases:

a) **Developer** for keycloak themes, extensions and image

1) build and integration-test with test-containers (uses standard keycloak image)
2) run external keycloak with hotdeploy (theme, extension, ...), run integrationtest, e2e testing

a) **Developer** publishing an image:

1) Standard keycloak docker image with [extensions](./keycloak-extensions), themes und server config.
2) Slim custom docker image with extensions, themes und server config (basis alpine) chose jdk version, base-os image version, base keycloak version.

c) **Tester/Developer** acceptance/e2e-testing with cypress

d) **Operator** configuring realm and server for different stages

## Some Highlights
- Extension: Custom REST Endpoint the can expose additional custom APIs: `CustomResource`
- Support for deploying extensions to running Keycloak container
- Support for instant reloading of theme and extension code changes
- Support Keycloak configuration customization via CLI scripts
- Examples for Integration Tests with [Keycloak-Testcontainers](https://github.com/dasniko/testcontainers-keycloak)
- Example for End to End Tests with [Cypress](https://www.cypress.io/)
- Realm configuration as Configuration as Code via [keycloak-config-cli](https://github.com/adorsys/keycloak-config-cli) 
- Multi-realm setup example with OpenID Connect and SAML based Identity Brokering
- LDAP based User Federation backed by [Docker-OpenLDAP](https://github.com/osixia/docker-openldap)
- Mail Server integration backed by [MailHog](https://github.com/mailhog/MailHog)
- TLS Support

## Usage prerequisits


| Tool | Version
|------|--------
| Java | 11
| mvn  | 3.6
| docker | 20.10
| docker-composer | 1.29 

Create a testrun folder to hold keycloak data.
```
mkdir -p testrun/data
```

Other required folders are created by build.

# Build
The project can be build with the following maven command:
```
mvn clean verify
```

## Build with Integration Tests
The example can be build with integration tests by running the following maven command:
```
mvn clean verify -Pwith-integration-tests
```

# Run

To speed up development we can mount the keycloak-extensions class-folder and keycloak-themes folder into
a Keycloak container that is started via docker-compose (see the start-scripts described below). This allows for quick turnarounds while working on themes
and extensions.

The default Keycloak admin username is `admin` with password `admin`.

## Start with plain HTTP

You can start the Keycloak container via:
```
./start-http.sh
```
Keycloak will be available on http://localhost:8080/auth.

## Start with HTTPS

### Preparation
Generate a certificate and Key for the example domain `acme.test` with [mkcert](https://github.com/FiloSottile/mkcert).
```
./bin/createTlsCerts.sh
```
This will generate a TLS certificate and key file in `.pem` format in `config/stage/dev/tls`.

Register map the following host names in your hosts configuration:
```
127.0.0.1 acme.test id.acme.test apps.acme.test admin.acme.test
```
#### Start
```
./start-tls.sh
```
Keycloak will be available on https://id.acme.test:8443/auth.

Note that after changing extensions code you need to run the `bin/triggerDockerExtensionDeploy.sh` script to trigger a redeployment of the custom extension by Keycloak.

# Build Docker Image
To build a custom Keycloak Docker image that contains the custom extensions and themes, you can run the following command:
```
mvn clean verify -Pwith-integration-tests io.fabric8:docker-maven-plugin:build
```

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
## Use the custom Docker Image in integration-test or start?

Replace the occurency of **quay.io/keycloak/keycloak:13.0.1** in docker-compose.yml for using the image with the start*-scripts. 
A tester might

Replace the occurency of **quay.io/keycloak/keycloak:13.0.1** in KeycloakTestSupport.java for using the image during the integration-tests.
Does not make much sense, but would work.

# Run End to End Tests

The [cypress](https://www.cypress.io/) based End to End tests can be found in the [keycloak-e2e](./keycloak-e2e) folder. 

To run the e2e tests, start the Keycloak environment and run the following commands:
```
cd keycloak-e2e
yarn run cypress:open
# yarn run cypress:test
```

# Example environment

## Realms

The example environment contains several realms to illustrate the interaction of different realms.

### Acme-Apps Realm

The `acme-apps` realm contains a simple demo application and provides integration with the `acme-internal`, `acme-ldap`
and `acme-saml` realm via Identity Brokering. The idea behind this setup is to provide a global
`acme-apps` realm for applications that are shared between internal and external users.

The `acme-internal` realm provides applications that are only intended for internal users.
The `acme-ldap` realm provides applications that are only intended for employees.
The `acme-internal` and `acme-ldap` realms serve as an OpenID Connect based Identity Provider for the `acme-apps` realm.
The `acme-saml` realm provides applications is similar to the `acme-internal` and serves as 
a SAML based Identity Provider for the `acme-apps` realm.

### Acme-Internal Realm

The `acme-internal` realm contains a test users which are stored in the Keycloak database.

Users:
- Username `tester` and password `test` (from database)

### Acme-LDAP Realm

The `acme-ldap` realm contains a test user and is connected to a federated user store (LDAP directory) provided via openldap.

- Username `FleugelR` and password `Password1` (from LDAP federation)

### Acme-SAML Realm

The `acme-saml` realm contains a test user and stores the users in the Keycloak database.

Users:
- Username `acmesaml` and password `test` (from database)

### Example App

A simple demo app can be used to show information from the Access-Token, ID-Token and UserInfo endpoint provided by Keycloak.

The demo app can be started by running `etc/runDemoApp.sh` and will be accessible via http://localhost:4000.

# Scripts

## Check prequisits

To manually check if all prequisits are fulfilled.
```
bin/prerequisits.sh
```

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

We use [aquasec/trivy](https://github.com/aquasecurity/trivy) to scan the generated docker image for vulnerabilities.

```
bin/scanImage.sh thomasdarimont/custom-keycloak:1.0.0-SNAPSHOT
```

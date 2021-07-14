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

## Usage prerequisites

| Tool | Version
|------|--------
| Java | 11
| mvn  | 3.6
| docker | 20.10
| docker-compose | 1.29 

# Development Environment

## Build
The project can be build with the following maven command:
```
mvn clean verify
```

### Build with Integration Tests
The example can be build with integration tests by running the following maven command:
```
mvn clean verify -Pwith-integration-tests
```

## Run

We provide a platform agnostic single-file source-code Java launcher [start.java](start.java) to start the Keycloak environment.

To speed up development we can mount the [keycloak/extensions](keycloak/extensions) class-folder and [keycloak/themes](keycloak/themes) folder into
a Keycloak container that is started via docker-compose (see below). This allows for quick turnarounds while working on themes and extensions.

The default Keycloak admin username is `admin` with password `admin`.

### Run with HTTP

You can start the Keycloak container via:
```
java start.java
```
Keycloak will be available on http://localhost:8080/auth.

### Enable HTTPS

The example environment can be configured with https via the `--https` flag.

#### Preparation
Generate a certificate and Key for the example domain `acme.test` with [mkcert](https://github.com/FiloSottile/mkcert).
```
java bin/createTlsCerts.java
```
This will generate a TLS certificate and key file in `.pem` format in `config/stage/dev/tls`.

Register map the following host names in your hosts configuration:
```
127.0.0.1 acme.test id.acme.test apps.acme.test admin.acme.test
```
#### Run with HTTPS
```
java start.java --https
```
Keycloak will be available on https://id.acme.test:8443/auth.

Note that after changing extensions code you need to run the `java bin/triggerDockerExtensionDeploy.java` script to trigger a redeployment of the custom extension by Keycloak.

### Enable OpenLDAP

The example environment can be configured with OpenLDAP via the `--openldap` flag.

#### Run with OpenLDAP
```
java start.java --openldap
```

### Enable PostgreSQL

The example environment can be configured to use PostgreSQL as a database via the `--database=postgres` flag to override the default `h2` database.

#### Run with PostgreSQL
```
java start.java --database=postgres
```

## Acme Example Realm Configuration

### Realms

The example environment contains several realms to illustrate the interaction of different realms.

#### Acme-Apps Realm

The `acme-apps` realm contains a simple demo application and provides integration with the `acme-internal`, `acme-ldap`
and `acme-saml` realm via Identity Brokering. The idea behind this setup is to provide a global
`acme-apps` realm for applications that are shared between internal and external users.

The `acme-internal` realm provides applications that are only intended for internal users.
The `acme-ldap` realm provides applications that are only intended for employees.
The `acme-internal` and `acme-ldap` realms serve as an OpenID Connect based Identity Provider for the `acme-apps` realm.
The `acme-saml` realm provides applications is similar to the `acme-internal` and serves as 
a SAML based Identity Provider for the `acme-apps` realm.

#### Acme-Internal Realm

The `acme-internal` realm contains a test users which are stored in the Keycloak database.

Users:
- Username `tester` and password `test` (from database)
- Username `support` and password `test` (from database)

The support user has access to a [dedicated realm scoped admin-console](https://www.keycloak.org/docs/latest/server_admin/index.html#_per_realm_admin_permissions) and can perform user and group lookups.
An example for a realm scoped admin-console URL is: `https://id.acme.test:8443/auth/admin/acme-internal/console`.

#### Acme-LDAP Realm

The `acme-ldap` realm contains a test user and is connected to a federated user store (LDAP directory) provided via openldap.

- Username `FleugelR` and password `Password1` (from LDAP federation)

#### Acme-SAML Realm

The `acme-saml` realm contains a test user and stores the users in the Keycloak database.

Users:
- Username `acmesaml` and password `test` (from database)

#### Example App

A simple demo app can be used to show information from the Access-Token, ID-Token and UserInfo endpoint provided by Keycloak.

The demo app is started and will be accessible via http://localhost:4000/?realm=acme-internal or https://apps.acme.test:4443/?realm=acme-internal.

# Deployment

## Custom Docker Image

### Build a custom Docker Image 

The dockerfile for the docker image build uses the [keycloak/Dockerfile.plain](keycloak/docker/src/main/docker/keycloak/Dockerfile.plain) by default.

To build a custom Keycloak Docker image that contains the custom extensions and themes, you can run the following command:
```bash
mvn clean verify -Pwith-integration-tests io.fabric8:docker-maven-plugin:build
```
The dockerfile can be customized via `-Ddocker.file=keycloak/Dockerfile.alpine-slim` after `mvn clean verify`.
It is also possible to configure the image name via `-Ddocker.image=acme/acme-keycloak2`.

### Running the custom Docker Image locally

The custom docker image created during the build can be stared with the following command:
```
docker run \
--name acme-keycloak \
-e KEYCLOAK_USER=admin \
-e KEYCLOAK_PASSWORD=admin \
-e KEYCLOAK_CONFIG_FILE=standalone-ha.xml \
-v $PWD/imex:/opt/jboss/imex:z \
-it \
--rm \
-p 8080:8080 \
acme/acme-keycloak:latest
```
# Testing
## Run End to End Tests

The [cypress](https://www.cypress.io/) based End to End tests can be found in the [keycloak-e2e](./keycloak-e2e) folder. 

To run the e2e tests, start the Keycloak environment and run the following commands:
```
cd keycloak-e2e
yarn run cypress:open
# yarn run cypress:test
```


# Scripts

## Check prequisits

To manually check if all prequisits are fulfilled.
```
java bin/prerequisites.bin
```

## Manually Trigger Extension Deployment

To manually trigger an extension redeployment after extension code changes / build, you can run the following script:
```
java bin/deployExtensions.java
```

## Import-/Exporting a Realm

To import/export of an existing realm as JSON start the docker-compose infrastructure and run the following script.
The export will create a file like `acme-apps-realm.json` in the `./keycloak/imex` folder.

```
java bin/realm.java --realm=acme-apps
```

The import would search an file `acme-apps-realm.json` in the `./keycloak/imex` folder.
```
java bin/realm.java --realm=acme-apps --action=import
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
- Program: `$JDKPath$/bin/java`
- Arguments: `$ProjectFileDir$/bin/deployExtensions.java`
- Working directory: `$ProjectFileDir$`
- Only select: `Synchronize files after execution.`

The extensions can now be redeployed by running `Tools -> External Tools -> kc-deploy-extensions`

## Add external tool in IntelliJ to trigger realm configuration

Instead of running the Keycloak Config CLI script yourself, you can register it as an external tool in IntelliJ as shown below.

- Name: `kc-deploy-config`
- Description: `Deploy Realm Config to Keycloak Docker Container`
- Program: `$JDKPath$/bin/java`
- Arguments: `$ProjectFileDir$/bin/applyRealmConfig.java`
- Working directory: `$ProjectFileDir$`
- Only select: `Synchronize files after execution.`

The extensions can now be redeployed by running `Tools -> External Tools -> kc-deploy-config`

## Scan Image for Vulnerabilities

We use [aquasec/trivy](https://github.com/aquasecurity/trivy) to scan the generated docker image for vulnerabilities.

```
java bin/scanImage.java --image-name=acme/acme-keycloak:latest
```

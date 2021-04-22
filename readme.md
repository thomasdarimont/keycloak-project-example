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
docker-compose --env-file custom-keycloak.env up
```

## Run custom Docker Image
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

The example environment contains a Keycloak realm named `custom`, which contains a simple demo application as well as a test user.
The test user has the username `tester` and password `test`.

### Example App

A simple demo app can be used to show information from the Access-Token, ID-Token and UserInfo endpoint provided by Keycloak.

The demo app can be started by running `etc/runDemoApp.sh` and will be accessible via http://localhost:4000.

# Scripts

## Manually Trigger Extension Deployment
```
bin/triggerDockerExtensionDeploy.sh
```

## Exporting the 'custom' Realm
```
bin/exportRealm.sh
```

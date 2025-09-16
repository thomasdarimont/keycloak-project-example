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
These requirements work in different contexts, roles and use-cases:

a) **Developer** for keycloak themes, extensions and image

1) build and integration-test with test-containers (uses standard keycloak image)
2) run external keycloak with hot-deploy (theme, extension, ...), run integrationtest, e2e testing

a) **Developer** publishing an image:

1) Standard keycloak docker image with [extensions](./keycloak-extensions), themes und server config.
2) Slim custom docker image with extensions, themes und server config (basis alpine) chose jdk version, base-os image version, base keycloak version.

c) **Tester/Developer** acceptance/e2e-testing with cypress

d) **Operator** configuring realm and server for different stages

## Some Highlights
- Extensions: SMS Authenticator, Backup-Codes, Remote Claim Mapper, Audit Event Listener, and Custom REST Endpoint the can expose custom endpoints: `CustomResource`
- Support for deploying extensions to a running Keycloak container
- Support for instant reloading of theme and extension code changes
- Support Keycloak configuration customization via CLI scripts
- Examples for Integration Tests with [Keycloak-Testcontainers](https://github.com/dasniko/testcontainers-keycloak)
- Example for End-to-End Tests with [Cypress](https://www.cypress.io/)
- Realm configuration as Configuration as Code via [keycloak-config-cli](https://github.com/adorsys/keycloak-config-cli)
- Example configurations to run Keycloak against different databases (PostgreSQL, MySQL, Oracle, MSSQL)
- Multi-realm setup example with OpenID Connect and SAML based Identity Brokering
- LDAP based User Federation backed by [Docker-OpenLDAP](https://github.com/osixia/docker-openldap)
- Mail Server integration backed by [maildev](https://github.com/maildev/maildev)
- TLS Support
- Support for exposing metrics via smallrye-metrics
- Examples for running a cluster behind a reverse proxy with examples for [HAProxy](deployments/local/cluster/haproxy), [Apache](deployments/local/cluster/apache), [nginx](deployments/local/cluster/nginx), [caddy](deployments/local/cluster/caddy)
- Examples for running a Keycloak cluster with an external infinispan cluster with [remote cache store](deployments/local/cluster/haproxy-external-ispn/docker-compose-haproxy-ispn-remote.yml) and [hotrod cache store](deployments/local/cluster/haproxy-external-ispn/docker-compose-haproxy-ispn-hotrod.yml).
- Example for Keycloak with [Graylog](https://www.graylog.org/) for log analysis, dashboards and alerting.
- Example for metrics collection and dashboards with [Prometheus](https://prometheus.io) and [Grafana](https://grafana.com/oss).
- Example for tracing with [OpenTelemetry](https://opentelemetry.io/) and [Jaeger](https://www.jaegertracing.io/)

## Usage envcheck

| Tool | Version
|------|--------
| Java | 21
| mvn  | 3.9
| docker | 24.0 (with docker compose)

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

We provide a platform-agnostic single-file source-code Java launcher [start.java](start.java) to start the Keycloak environment.

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
# AND 
java bin/createTlsCerts.java --pkcs12 --keep
```
This will generate a TLS certificates and key file in `.pem` format in `config/stage/dev/tls`.
The later command will create a certificate in `.p12` PKCS12 format, which will be used as a custom truststore by Keycloak.  

Register map the following host names in your hosts file configuration, e.g. `/etc/hosts` on linux / OSX or `c:\Windows\System32\Drivers\etc\hosts` on Windows:
```
127.0.0.1 acme.test id.acme.test apps.acme.test admin.acme.test ops.acme.test
```
#### Run with HTTPS
```
java start.java --https
```
The Keycloak admin-console will be available on https://admin.acme.test:8443/auth/admin.

Note that after changing extensions code you need to run the `java bin/triggerDockerExtensionDeploy.java` script to trigger a redeployment of the custom extension by Keycloak.

### Enable OpenLDAP

The example environment can be configured with OpenLDAP via the `--openldap` flag.

#### Run with OpenLDAP
```
java start.java --openldap
```

### Enable Postgresql

The example environment can be configured to use Postgresql as a database via the `--database=postgres` flag to override the default `h2` database.

#### Run with Postgresql
```
java start.java --database=postgres
```

### Access metrics

The example environment includes an smallrye-metrics and eclipse-metrics integration for wildfly.

Metrics are exposed via the wildfly management interface on http://localhost:9990/metrics

Realm level metrics are collected by a custom `EventListenerProvider` called `metrics`. 

### Enable Graylog

The example environment can be configured to send Keycloak's logout output to Graylog via the `--logging=graylog` option.

Note that you need to download the [`logstash-gelf` wildfly module](https://search.maven.org/remotecontent?filepath=biz/paluch/logging/logstash-gelf/1.14.1/logstash-gelf-1.14.1-logging-module.zip)
and unzip the libraries into the [deployments/local/dev/graylog/modules](deployments/local/dev/graylog/modules) folder.

```
cd deployments/local/dev/graylog/modules
wget -O logstash-gelf-1.14.1-logging-module.zip https://search.maven.org/remotecontent?filepath=biz/paluch/logging/logstash-gelf/1.14.1/logstash-gelf-1.14.1-logging-module.zip
unzip -o logstash-gelf-1.14.1-logging-module.zip
rm *.zip
```

#### Run with Graylog
```
java start.java --logging=graylog
```

### Enable Prometheus

Prometheus can scrape0 metrics from configured targets and persists the collected data in a time series database.
The metrics data can be used to create monitoring dashboards with tools like grafana (see  [Grafana](#enable-grafana)).

Scrape targets configured:

|System| Target                                 |Additional Labels
|------|----------------------------------------|------
|keycloak | http://acme-keycloak:8080/auth/metrics | env

#### Run with Prometheus
```
java start.java --metrics=prometheus
```

### Enable Grafana

Grafana supports dashboards and alerting based on data from various datasources.

Note: To enable grafana with tls, a permission change is required as docker does not support a way to map users for shared files.
You need to add read permissions for the key file `acme.test+1-key.pem` in config/stage/dev/tls for the group of the current user.

Access to Grafana can be configured in multiple ways, even a login with Keycloak is possible. 
In this example we use configured admin user account to access Grafana, but we also offer a login via Keycloak by leveraging the generic OAuth integration.
Grafana is configured to not allow login as guest.

#### Run with Grafana
```
java start.java --grafana
```

Open [Grafana](https://apps.acme.test:3000/grafana)

Manual steps when logged in as an Admin (Example User: devops_fallback, Password: test)
* Configure datasource
    * Add e.g. prometheus as datasource (http://acme-prometheus:9090/ installed by default) (see [Grafana](#enable-prometheus))
    * Add e.g. elastic-search as datasource (http://acme-graylog-lo:9090/) (see [Graylog](#enable-graylog) services)
* Import Boards of your choice from [Grafana](https://grafana.com/grafana/dashboards) (for testing an [exported board](../../../config/stage/dev/grafana/microprofile-wildfly-16-metrics_rev1.json) can be used) 

### Enable Tracing
With [OpenTelemetry](https://opentelemetry.io/) and [Jaeger](https://www.jaegertracing.io/), it is possible to trace requests traveling through Keycloak and the systems integrating it.
This uses the Quarkus OpenTelemetry extension in order to create traces, which are then sent to the [otel-collector](https://opentelemetry.io/docs/collector/).
The collector then passes the information on to Jaeger, where they can be viewed in the web interface

#### Run with Tracing
```
java start.java --tracing
```
Open [Jaeger](http://ops.acme.test:16686) or [Jaeger with TLS](https://ops.acme.test:16686), depending on configuration.
When TLS is enabled, it is enabled for all three of the following:
* Jaeger UI
* Keycloak -> Collector communication
* Collector -> Jaeger communication

#### Instrumentation
In order to gain additional insights, other applications that integrate with Keycloak can also send traces to the collector.
The [OpenTelemetry Documentation](https://opentelemetry.io/docs/instrumentation/) contains tools to instrument applications in various languages.

You can use the `bin/downloadOtel.java` scrtipt to download the otel agent.

Quarkus applications like Keycloak can also use the [Quarkus OpenTelemetry extension](https://quarkus.io/guides/opentelemetry) instead of the agent.
An example for running an instrumented Spring Boot app could look like this:
```
OTEL_METRICS_EXPORTER=none \
OTEL_SERVICE_NAME="frontend-webapp-springboot" \
OTEL_PROPAGATORS="b3multi" \
OTEL_EXPORTER_OTLP_ENDPOINT="http://id.acme.test:4317" \
java -javaagent:bin/opentelemetry-javaagent.jar \
-jar apps/frontend-webapp-springboot/target/frontend-webapp-springboot-0.0.1-SNAPSHOT.jar
```
The included IDEA run-config for the frontend-webapp-springboot module contains the necessary configuration to run that module with tracing enabled.
If you then navigate to the [frontend webapp](https://apps.acme.test:4633/webapp/), you can navigate through the application, and then later check the Jaeger UI for traces.

### Clustering

Clustering examples can be found in the [deployments/local/cluster](deployments/local/cluster) folder.

### Running with non-default docker networks

Some features of this project setup communicate with services inside the docker stack through the host.
By default, the IP of the host in Docker is `172.17.0.1`, but this can be changed by configuration.
One reason to change it is because Wi-Fi on ICE trains uses IP addresses from the same network.
An example for a changed setup from `/etc/docker/daemon.json` can look like this:

````json
{
    "default-address-pools":
    [
        {"base":"172.19.0.0/16","size":24}
    ]
}
````
In this case, the host IP is `172.19.0.1`, which can be configured for the project using the start option `--docker-host=172.19.0.1`

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
An example for a realm scoped admin-console URL is: `https://admin.acme.test:8443/auth/admin/acme-internal/console`.

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

To build the image with Keycloak.X use:
```
mvn clean package -DskipTests -Ddocker.file=keycloakx/Dockerfile.plain io.fabric8:docker-maven-plugin:build
```

### Running the custom Docker Image locally

The custom docker image created during the build can be stared with the following command:
```
docker run \
--name acme-keycloak \
-e KEYCLOAK_ADMIN=admin \
-e KEYCLOAK_ADMIN_PASSWORD=admin \
-e KC_HTTP_RELATIVE_PATH=auth \
-it \
--rm \
-p 8080:8080 \
acme/acme-keycloak:latest \
start-dev \
--features=preview
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

## Check prerequisites

To manually check if all prerequisites are fulfilled.
```
java bin/envcheck.java
```

## Import-/Exporting a Realm

To import/export of an existing realm as JSON start the docker-compose infrastructure and run the following script.
The export will create a file like `acme-apps-realm.json` in the `./keycloak/imex` folder.

```
java bin/realmImex.java --realm=acme-internal --verbose
```

The import would search an file `acme-apps-realm.json` in the `./keycloak/imex` folder.
```
java bin/realmImex.java --realm=acme-internal --verbose --action=import
```

# Tools

## maildev

Web Interface: http://localhost:1080/mail
Web API: https://github.com/maildev/maildev/blob/master/docs/rest.md

## phpldapadmin

Web Interface: http://localhost:17080
Username: cn=admin,dc=corp,dc=acme,dc=local
Password: admin

# Misc

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

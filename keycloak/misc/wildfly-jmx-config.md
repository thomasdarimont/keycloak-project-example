How to connect to Keycloak via JMX
----

# Setup
## Create a management user for JMX
See deployments/local/dev/keycloak/Dockerfile
```
docker exec -it dev_acme-keycloak_1 /opt/jboss/keycloak/bin/add-user.sh jmxuser password
```


## Export jboss-cli-client.jar locally
```
docker cp dev_acme-keycloak_1:/opt/jboss/keycloak/bin/client/jboss-client.jar .
```

# VisualVM

## Start VisualVM with jboss-client.jar
```
visualvm -cp:a ./jboss-client.jar
```

## Create new JMX Connection in VisualVM

JMX URL: `service:jmx:http-remoting-jmx://localhost:9990`
Username: `jmxuser`
Password: `password`
Do not require SSL: on (for the demo...)

# Java Mission Control (JMC)

## Add jboss-cli-client.jar bundle to JMC

Currently JMC cannot be used with the plain `jboss-client.jar` since it is lacking some osgi bundle metadata. 

As a workaround we create a patched `jboss-client.jar` with the missing osgi bundle metadata.

We create a file with the additional osgi bundle metadata, e.g.: `jboss-jmx.mf`:
```
Bundle-ManifestVersion: 2 
Bundle-SymbolicName: org.jboss.client
Bundle-Version: 1.0
Bundle-Name: JBoss Client Library
Fragment-Host: org.openjdk.jmc.rjmx
Export-Package: *
Automatic-Module-Name: org.jboss.client
```

Then we create a patched local version of the `jboss-client.jar`.
```
cp /home/tom/dev/playground/keycloak/keycloak-16.1.0/bin/client/jboss-client.jar .

jar -ufm ./jboss-client.jar jboss-jmx.mf

cp ./jboss-client.jar "/home/tom/.sdkman/candidates/jmc/8.1.1.51-zulu/Azul Mission Control/dropins"
```

We then copy the `jboss-client.jar` file into the `dropins` folder of JMC:
```
cp ./jboss-client.jar /path/to/jmc/dropins/
```

We can then start JMC and create a new JMX connection as shown below.

See:
- https://access.redhat.com/solutions/5897561
- https://github.com/thomasdarimont/keycloak-jmx-jmc-poc

## Create new JMX Connection in Java Mission Control

JMX URL: `service:jmx:http-remoting-jmx://localhost:9990`
Username: `jmxuser`
Password: `password`

How to connect to Keycloak.X via JMX
----

# Setup
## Create a management user for JMX

See `keycloak/config/jmxremote.password`, e.g. `controlRole`.

# VisualVM

[VisualVM](https://visualvm.github.io/)

## Start VisualVM
```
visualvm
```

## Create new JMX Connection in VisualVM

- JMX URL: `localhost:8790` or `service:jmx:rmi:///jndi/rmi://localhost:8790/jmxrmi`
- Username: `controlRole`
- Password: `password`
- Do not require SSL: `on` (for the demo...)

# Java Mission Control (JMC)

[Java Mission Control](https://openjdk.java.net/projects/jmc/)

## Start Java Mission Control
```
jmc
```

## Create new JMX Connection in Java Mission Control

- JMX URL: `localhost:8790` or `service:jmx:rmi:///jndi/rmi://localhost:8790/jmxrmi`
- Username: `controlRole`
- Password: `password`

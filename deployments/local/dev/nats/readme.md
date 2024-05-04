NATS Support
---

```
nats context add localhost --description "Localhost"
```

Add username / password in context config
```
vi ~/.config/nats/context/localhost.json
```

List contexts
```
nats context ls
```

Select context
```
nats ctx select localhost
```

Nats subscribe to keycloak subject
```
nats sub acme.iam.keycloak>
```
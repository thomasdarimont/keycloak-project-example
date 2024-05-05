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

Nats subscribe to subject with prefix
```
nats sub "acme.iam.keycloak.>"
```


---

# Misc

## Create stream
```
nats stream add KEYCLOAK --subjects "acme.iam.keycloak.*" --ack --max-msgs=-1 --max-bytes=-1 --max-age=1y --storage file --retention limits --max-msg-size=-1 --discard=old

nats stream info KEYCLOAK
```

## Add consumers
```
nats consumer add KEYCLOAK USER --filter "acme.iam.keycloak.user" --ack explicit --pull --deliver all --max-deliver=-1 --sample 100
nats consumer add KEYCLOAK ADMIN --filter "acme.iam.keycloak.admin" --ack explicit --pull --deliver all --max-deliver=-1 --sample 100
nats consumer add KEYCLOAK MONITOR --filter '' --ack none --target monitor.KEYCLOAK --deliver last --replay instant
```

## Stream Status

https://docs.nats.io/running-a-nats-service/configuration/clustering/jetstream_clustering/administration

```
nats stream report

nats server report jetstream --user "admin" --password "password"
```

## Read consumer

```
nats consumer next KEYCLOAK USER --count 1000
```

## Subscribe to subject
```
nats sub "acme.iam.keycloak.user" --translate 'jq .' --count 10
```

---

# Misc

nats stream add IOT --subjects "iot.*" --ack --max-msgs=-1 --max-bytes=-1 --max-age=1y --storage file --retention limits --max-msg-size=-1 --discard=old

nats consumer add IOT CMD --filter "iot.cmd" --ack explicit --pull --deliver last --max-deliver=-1 --sample 100

nats consumer next IOT CMD --count 3

nats pub iot.cmd --count=6 --sleep 1s "iot cmd #{{Count}} @ {{TimeStamp}}"

nats sub iot.cmd --last
nats sub iot.cmd --new

nats sub "iot.*" --last-per-subject
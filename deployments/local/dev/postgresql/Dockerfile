FROM postgres:16.6

# 999 uid of postgresql
COPY --chown=999:0 "./acme.test+1.pem" /var/lib/postgresql/server.crt
COPY --chown=999:0 "./acme.test+1-key.pem" /var/lib/postgresql/server.key

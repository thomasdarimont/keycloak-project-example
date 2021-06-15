FROM haproxy:2.4.0-alpine

COPY --chown=haproxy:haproxy "./acme.test+1.pem" "/etc/haproxy/haproxy.crt.pem"
COPY --chown=haproxy:haproxy "./acme.test+1-key.pem" "/etc/haproxy/haproxy.crt.pem.key"
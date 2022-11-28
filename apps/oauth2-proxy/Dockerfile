FROM quay.io/oauth2-proxy/oauth2-proxy:v7.4.0-amd64

USER 0

COPY --chown=65532:0 "./acme.test+1.pem" /cert.pem
COPY --chown=65532:0 "./acme.test+1-key.pem" /cert.key

USER 65532

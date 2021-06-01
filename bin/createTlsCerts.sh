#!/usr/bin/env bash

DOMAIN=${DOMAIN:-acme.test}

echo "Generating TLS Certificate and Key for domain: $DOMAIN"

pushd .
cd ./config/stage/dev/tls
rm -f *.pem
mkcert -install $DOMAIN "*.$DOMAIN"
echo "TLS Certificate and Key created in $PWD"
ls -Al *.pem

popd
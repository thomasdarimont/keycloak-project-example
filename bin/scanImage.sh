#!/usr/bin/env bash

IMAGE_NAME=${IMAGE_NAME:-thomasdarimont/custom-keycloak:1.0.0-SNAPSHOT}

echo "Scanning Image: $IMAGE_NAME"

docker run \
  --privileged \
  --rm \
  -v /var/run/docker.sock:/var/run/docker.sock:z aquasec/trivy:0.18.3 \
  $IMAGE_NAME
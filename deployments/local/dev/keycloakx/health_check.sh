#!/bin/bash

# Define the host and port
HOST="localhost"
PORT="8080"
URL="/auth/health"

# Send the HTTP request and store the response
RESPONSE=$(exec 3<>/dev/tcp/$HOST/$PORT && echo -e "GET $URL HTTP/1.1\r\nHost: $HOST\r\nConnection: close\r\n\r\n" >&3 && cat <&3)

# Check if the response contains "HTTP/1.1 200 OK"
if echo "$RESPONSE" | grep -q "HTTP/1.1 200 OK"; then
  exit 0
else
  exit 1
fi
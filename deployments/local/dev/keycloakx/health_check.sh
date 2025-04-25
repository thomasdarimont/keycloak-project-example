#!/bin/bash

# Send the HTTP request and store the response
RESPONSE=$(curl -k -v "https://localhost:9000/auth/health")

# Check if the response contains "HTTP/1.1 200 OK"
if echo "$RESPONSE" | grep -q "\"status\": \"UP\""; then
  exit 0
else
  exit 1
fi
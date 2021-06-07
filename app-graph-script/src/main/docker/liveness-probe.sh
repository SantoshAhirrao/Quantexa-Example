#!/bin/sh

PORT=${1}
HTTP=$(curl http://localhost:${PORT}/health 2>/dev/null)
HTTPS=$(curl --insecure https://localhost:${PORT}/health 2>/dev/null)
RESULT=$HTTP$HTTPS

if echo $RESULT | grep -Eq '"status":"UP"'; then
    echo "SUCCESS"
  exit 0
else
    echo "FAILURE"
  exit 1
fi
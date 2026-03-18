#!/usr/bin/env bash
set -euo pipefail

TOMCAT_HOME="/usr/bin/spBuild/opt/tomcat"
TOMCAT_BIN="$TOMCAT_HOME/bin"
IMPORT_SCRIPT="$TOMCAT_HOME/importxml.sh"

TOMCAT_URL="http://localhost:8080/identityiq/login.jsf"
MAX_WAIT=300   # seconds
SLEEP=5

echo "Starting Tomcat..."
"$TOMCAT_BIN/startup.sh"

echo "Waiting for Tomcat to be ready at $TOMCAT_URL"

elapsed=0
until curl -sf "$TOMCAT_URL" >/dev/null; do
  sleep "$SLEEP"
  elapsed=$((elapsed + SLEEP))

  if [ "$elapsed" -ge "$MAX_WAIT" ]; then
    echo "ERROR: Tomcat did not start within ${MAX_WAIT}s"
    exit 1
  fi

  echo "Tomcat not ready yet... waited ${elapsed}s"
done

echo "Tomcat is up"

echo "Running importxml.sh"
"$IMPORT_SCRIPT"

echo "Import completed"


#!/usr/bin/env bash
set -euo pipefail

TOMCAT_HOME="/usr/bin/spBuild/opt/tomcat"
TOMCAT_BIN="$TOMCAT_HOME/bin"
IMPORT_SCRIPT="$TOMCAT_HOME/importxml.sh"
TOMCAT_PATTERN="org.apache.catalina.startup.Bootstrap"

TOMCAT_URL="http://localhost:8080/identityiq/login.jsf"
MAX_WAIT=300
SLEEP=5

echo "Checking Tomcat is not already running..."
if pgrep -f "$TOMCAT_PATTERN" >/dev/null; then
  echo "ERROR: Tomcat process already running before startup"
  pgrep -af "$TOMCAT_PATTERN" || true
  exit 1
fi

echo "Starting Tomcat..."
"$TOMCAT_BIN/startup.sh"

sleep 5

COUNT=$(pgrep -f "$TOMCAT_PATTERN" | wc -l | tr -d ' ')
if [ "$COUNT" -ne 1 ]; then
  echo "ERROR: Expected 1 Tomcat process after startup, found $COUNT"
  pgrep -af "$TOMCAT_PATTERN" || true
  exit 1
fi

echo "Waiting for Tomcat to be ready at $TOMCAT_URL"

elapsed=0
until curl -sf "$TOMCAT_URL" >/dev/null; do
  sleep "$SLEEP"
  elapsed=$((elapsed + SLEEP))

  if [ "$elapsed" -ge "$MAX_WAIT" ]; then
    echo "ERROR: Tomcat did not start within ${MAX_WAIT}s"
    pgrep -af "$TOMCAT_PATTERN" || true
    exit 1
  fi

  echo "Tomcat not ready yet... waited ${elapsed}s"
done

echo "Tomcat is up"

echo "Running importxml.sh"
"$IMPORT_SCRIPT"

echo "Import completed"

echo "Waiting 30s for application stabilization..."
sleep 30

echo "Final Tomcat process check:"
pgrep -af "$TOMCAT_PATTERN" || true
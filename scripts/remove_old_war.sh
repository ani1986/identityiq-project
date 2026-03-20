#!/bin/bash
set -euo pipefail

TOMCAT_HOME="/usr/bin/spBuild/opt/tomcat"
TOMCAT_PATTERN="org.apache.catalina.startup.Bootstrap"
MAX_WAIT=120
SLEEP=5

echo "Stopping Tomcat gracefully..."
"$TOMCAT_HOME/bin/shutdown.sh" || true

elapsed=0
while pgrep -f "$TOMCAT_PATTERN" >/dev/null; do
  if [ "$elapsed" -ge "$MAX_WAIT" ]; then
    echo "Tomcat still running after ${MAX_WAIT}s, killing it..."
    pkill -f "$TOMCAT_PATTERN" || true
    sleep 10
    break
  fi

  echo "Waiting for Tomcat to stop... ${elapsed}s"
  sleep "$SLEEP"
  elapsed=$((elapsed + SLEEP))
done

if pgrep -f "$TOMCAT_PATTERN" >/dev/null; then
  echo "Tomcat still running after pkill, force killing..."
  pkill -9 -f "$TOMCAT_PATTERN" || true
  sleep 5
fi

if pgrep -f "$TOMCAT_PATTERN" >/dev/null; then
  echo "ERROR: Tomcat could not be stopped"
  pgrep -af "$TOMCAT_PATTERN" || true
  exit 1
fi

echo "Tomcat fully stopped"

echo "Removing existing WAR"
rm -f /usr/bin/spBuild/opt/tomcat/webapps/identityiq.war

echo "Removing exploded IdentityIQ directory"
rm -rf /usr/bin/spBuild/opt/tomcat/webapps/identityiq

echo "Cleanup complete"
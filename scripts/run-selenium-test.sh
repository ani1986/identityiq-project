#!/usr/bin/env bash
set -euo pipefail

cleanup_stale() {
  echo "Cleaning stale Selenium processes before run..."
  pkill -x chromedriver 2>/dev/null || true
  rm -rf /tmp/selenium-chrome-* || true
}

export IIQ_BASE_URL="http://localhost:8080/identityiq"
export IIQ_USERNAME="spadmin"
export IIQ_PASSWORD="Virtual@123"
export IIQ_TASK_NAME="HRMS Employee Aggregation"
export _JAVA_OPTIONS="-Djava.awt.headless=true"

WORK_DIR="/usr/bin/spBuild/iiq-selenium-ant"
ANT_BIN="${ANT_BIN:-$(command -v ant)}"
TIMEOUT_SECONDS=600
LOG_FILE="/usr/bin/spBuild/iiq-selenium-ant/iiq-selenium-$(date +%Y%m%d%H%M%S).log"

cleanup_stale

cd "$WORK_DIR"

if [ -z "${ANT_BIN:-}" ] || [ ! -x "$ANT_BIN" ]; then
  echo "ERROR: ant not found in PATH"
  exit 1
fi

echo "Running Selenium tests..."
echo "Working dir: $WORK_DIR"
echo "Log file: $LOG_FILE"

set +e
timeout "$TIMEOUT_SECONDS" "$ANT_BIN" clean run-tests | tee "$LOG_FILE"
EXIT_CODE=${PIPESTATUS[0]}
set -e

if [ "$EXIT_CODE" -eq 124 ]; then
  echo "ERROR: Test timed out after ${TIMEOUT_SECONDS}s"
  cleanup_stale
  exit 1
elif [ "$EXIT_CODE" -ne 0 ]; then
  echo "ERROR: Test failed with exit code $EXIT_CODE"
  cleanup_stale
  exit "$EXIT_CODE"
fi

echo "Test passed"
cleanup_stale
exit 0
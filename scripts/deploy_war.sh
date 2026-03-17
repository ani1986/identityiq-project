#!/bin/bash
set -euo pipefail

TOMCAT_WEBAPPS="/usr/bin/spBuild/opt/tomcat/webapps"
WAR_FILE="${TOMCAT_WEBAPPS}/identityiq.war"
IIQ_DIR="${TOMCAT_WEBAPPS}/identityiq"
BIN_DIR="${IIQ_DIR}/WEB-INF/bin"
IMPORT_FILE="${IIQ_DIR}/sp.init-custom.xml"

IIQ_USER="spadmin"
IIQ_PASS="Virtual@123"

echo "=== DEPLOY_WAR_SH VERSION MARKER 17-MAR-2026-V2 ==="

echo "Checking WAR exists"
ls -l "${WAR_FILE}"

echo "Creating IdentityIQ directory"
mkdir -p "${IIQ_DIR}"

echo "Extracting WAR into identityiq directory"
cd "${IIQ_DIR}"
jar -xf "${WAR_FILE}"

echo "Removing WAR after extraction"
rm -f "${WAR_FILE}"

echo "Navigating to WEB-INF/bin"
cd "${BIN_DIR}"

echo "Making iiq executable"
chmod +x ./iiq

echo "Checking import file"
ls -l "${IMPORT_FILE}"

echo "Running IdentityIQ console import with expect"
/usr/bin/expect <<EOF
set timeout 300
spawn ./iiq console
expect "User:"
send "${IIQ_USER}\r"
expect "Password:"
send "${IIQ_PASS}\r"
expect ">"
send "import ${IMPORT_FILE}\r"
expect ">"
send "quit\r"
expect eof
EOF

echo "Deployment script completed"

#!/bin/bash
set -euo pipefail
echo "=== DEPLOY_WAR_SH VERSION MARKER 17-MAR-2026-V1 ==="

TOMCAT_WEBAPPS="/usr/bin/spBuild/opt/tomcat/webapps"
WAR_FILE="${TOMCAT_WEBAPPS}/identityiq.war"
IIQ_DIR="${TOMCAT_WEBAPPS}/identityiq"
BIN_DIR="${IIQ_DIR}/WEB-INF/bin"
IMPORT_FILE="${IIQ_DIR}/sp.init-custom.xml"
CMD_FILE="${BIN_DIR}/iiq_commands.txt"

IIQ_USER="spadmin"
IIQ_PASS="Virtual@123"

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

echo "Preparing console input file"
printf '%s\n%s\nimport %s\nquit\n' "${IIQ_USER}" "${IIQ_PASS}" "${IMPORT_FILE}" > "${CMD_FILE}"

echo "Dumping console input file for verification"
cat -A "${CMD_FILE}"
od -An -tx1 -c "${CMD_FILE}"

echo "Running IdentityIQ console import"
./iiq console < "${CMD_FILE}"

echo "Deployment script completed"

#!/bin/bash
set -e

TOMCAT_WEBAPPS="/usr/bin/spBuild/opt/tomcat/webapps"
WAR_FILE="${TOMCAT_WEBAPPS}/identityiq.war"
IIQ_DIR="${TOMCAT_WEBAPPS}/identityiq"
BIN_DIR="${IIQ_DIR}/WEB-INF/bin"

echo "Checking WAR exists"
ls -l "${WAR_FILE}"

echo "Creating IdentityIQ directory"
mkdir -p "${IIQ_DIR}"

echo "Extracting WAR into identityiq directory"
cd "${IIQ_DIR}"
jar -xf "${WAR_FILE}"

echo "Removing WAR after extraction"
rm -f "${WAR_FILE}"

echo "Deployment script completed"

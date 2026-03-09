#!/bin/bash
echo "Stopping Tomcat"
/usr/bin/spBuild/opt/tomcat/bin/shutdown.sh

echo "Removing existing WAR"
rm -f /usr/bin/spBuild/opt/tomcat/webapps/identityiq.war

echo "Removing exploded IdentityIQ directory"
rm -rf /usr/bin/spBuild/opt/tomcat/webapps/identityiq

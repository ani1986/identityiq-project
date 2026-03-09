#!/bin/sh
export ANT_HOME=${ANT_HOME:-/opt/apache-ant-1.10.14}
export PATH="$ANT_HOME/bin:$PATH"
/opt/apache-ant-1.10.14/bin/ant $@

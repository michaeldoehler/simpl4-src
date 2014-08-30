#!/bin/sh
#
# Script to run Pax Runner, which starts OSGi frameworks with applications.
#
#

java $JAVA_OPTS -cp .:$REPOSITORY/pax-runner-1.8.5.jar org.ops4j.pax.runner.Run "$@"

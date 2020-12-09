#!/bin/bash
# builds the maven-repair plugin

set -e
export M2_HOME=/usr/local/maven

NPEFIX_VERSION=`xmlstarlet sel -t -v '//_:dependency[_:artifactId="npefix"]/_:version' src/maven-repair/pom.xml`

mvn clean install -B -f src/repairnator-core/ && mvn -Dtest=$TEST_LIST -DNPEFIX_VERSION=$NPEFIX_VERSION clean test -B -f $TEST_PATH -DskipTests
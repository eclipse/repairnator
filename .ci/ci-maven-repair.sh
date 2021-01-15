#!/bin/bash
# builds the maven-repair plugin

set -e
set -x
export M2_HOME=/usr/share/maven

NPEFIX_VERSION=`xmlstarlet sel -t -v '//_:dependency[_:artifactId="npefix"]/_:version' src/maven-repair/pom.xml`

mvn clean install -B -f src/repairnator-core/ -DskipTests && \
mvn clean install -B -f src/repairnator-pipeline/ -DskipTests && \
mvn ${MAVEN_OPTS} -Dtest=$TEST_LIST -DNPEFIX_VERSION=$NPEFIX_VERSION clean test -B -f $TEST_PATH

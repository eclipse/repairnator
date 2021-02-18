#!/usr/bin/env bash
# running the Java tests of Repairnator on Travis

set -e
set -x
export M2_HOME=/usr/share/maven

mvn clean install -B -f src/repairnator-core/ && \
mvn ${MAVEN_OPTS}  clean install -B -f src/maven-repair/ -DskipTests && \
mvn ${MAVEN_OPTS} -Dtest=$TEST_LIST clean test -B -f $TEST_PATH

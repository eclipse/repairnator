#!/usr/bin/env bash
# running the Java tests of Repairnator on Travis

set -e
set -x
export M2_HOME=/usr/share/maven

# sudo apt-get update
# sudo apt-get install -y xmlstarlet

mvn clean install -B -f src/repairnator-pipeline/ && mvn clean install -B -f src/repairnator-core/ && mvn ${MAVEN_OPTS} -Dtest=$TEST_LIST clean test -B -f $TEST_PATH -Dmaven.resolver.transport=native  -Daether.connector.connectTimeout=300000 -Daether.connector.requestTimeout=300000

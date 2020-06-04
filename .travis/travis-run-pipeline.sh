#!/usr/bin/env bash
# running the Java tests of Repairnator on Travis

set -e
export M2_HOME=/usr/local/maven

mvn clean install -B -f src/repairnator-core/ && mvn -Dtest=$TEST_LIST clean test -B -f $TEST_PATH

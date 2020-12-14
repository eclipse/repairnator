#!/usr/bin/env bash
# running the Java tests of Repairnator on Travis

set -e
set -x
export M2_HOME=/usr/local/maven

# sudo apt-get update
# sudo apt-get install -y xmlstarlet

mvn clean install -B -f src/repairnator-core/ && mvn -Dtest=$TEST_LIST clean test -B -f $TEST_PATH

#!/usr/bin/env bash
# running the Java tests of Repairnator on Travis

set -e
export M2_HOME=/usr/local/maven

cd repairnator

mvn clean test -B -X

# printing timing results to identify slow tests
grep -h time= */target/surefire-reports/*xml

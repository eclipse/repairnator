#!/usr/bin/env bash
# running the Java tests of Repairnator on Travis

set -e
export M2_HOME=/usr/local/maven

mvn clean test -B

cd ..
# printing timing results to identify slow tests
grep -h time= */target/surefire-reports/*xml

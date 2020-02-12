#!/usr/bin/env bash
# running the Java tests of Repairnator on Travis

set -e
export M2_HOME=/usr/local/maven

cd src

mvn clean test -B

# build and test jenkins plugin

cd repairnator-jenkins-plugin

mvn clean test

cd ..
# printing timing results to identify slow tests
grep -h time= */target/surefire-reports/*xml

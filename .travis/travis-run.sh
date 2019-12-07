#!/usr/bin/env bash
# running the Java tests of Repairnator on Travis

set -e
export M2_HOME=/usr/local/maven

if [[ $TRAVIS_PULL_REQUEST_SLUG == eclipse/repairnator ]]
then
  # the branch build is responsible for the tests, no need to have the PR build and the branch build (Eclipse's Travis account is very busy)
  exit
fi

cd repairnator

mvn clean test -B

# printing timing results to identify slow tests
grep -h time= */target/surefire-reports/*xml

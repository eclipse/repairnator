#!/usr/bin/env bash

set -e
export M2_HOME=/usr/local/maven

cd repairnator

./docker-images/bears-checkbranches-dockerimage/check_branches_test.sh

mvn clean install -Ptravis
mvn jacoco:report coveralls:report --fail-never
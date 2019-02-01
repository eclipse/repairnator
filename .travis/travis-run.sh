#!/usr/bin/env bash

set -e
export M2_HOME=/usr/local/maven

cd repairnator

mvn clean install -Ptravis
mvn jacoco:report coveralls:report --fail-never

./docker-images/checkbranches-dockerimage/check_branches_test.sh
./docker-images/bears-checkbranches-dockerimage/check_branches_test.sh
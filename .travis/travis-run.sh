#!/usr/bin/env bash

set -e
export M2_HOME=/usr/local/maven

cd repairnator
mvn clean install -Ptravis
mvn jacoco:report coveralls:report --fail-never


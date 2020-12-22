#!/usr/bin/env bash
# running the Java tests of Repairnator on Travis

set -e
export M2_HOME=/usr/share/maven

mvn clean test -B -f $TEST_PATH


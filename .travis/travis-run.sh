#!/usr/bin/env bash
# running the Java tests of Repairnator on Travis

set -e
export M2_HOME=/usr/local/maven

cd repairnator

# mvn clean test -B
mvn clean test -DfailIfNoTests=false -Dtest=fr.inria.spirals.repairnator.process.step.repair.TestSequencerRepair -Dsurefire.useFile=false

# printing timing results to identify slow tests
grep -h time= */target/surefire-reports/*xml

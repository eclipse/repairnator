#!/bin/bash

set -e

cd /root
mkdir github
cd github
git clone --recursive https://github.com/fermadeiral/repairnator.git

echo "LibRepair repository cloned."

cd repairnator/repairnator
git checkout create-launcher-utils
mvn clean install -DskipTests=true

echo "Repairnator compiled and installed"

cp repairnator-pipeline/target/repairnator-pipeline-*-with-dependencies.jar /root/repairnator-pipeline.jar

echo "Repairnator-pipeline jar file installed in /root directory"
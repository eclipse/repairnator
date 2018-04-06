#!/bin/bash

set -e

cd /root
mkdir github
cd github
git clone --recursive --depth=1 https://github.com/Spirals-Team/repairnator.git

echo "LibRepair repository cloned."

cd repairnator/repairnator
mvn clean install -DskipTests=true

#mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=fr.inria.repairnator:repairnator-pipeline:LATEST:jar:jar-with-dependencies -Ddest=/root/repairnator-pipeline.jar

echo "Repairnator-pipeline jar file installed in /root directory"
#!/bin/bash

set -e

mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=fr.inria.repairnator:repairnator-pipeline:LATEST:jar:jar-with-dependencies -Ddest=/root/repairnator-pipeline.jar

echo "Repairnator-pipeline jar file installed in /root directory"
#!/bin/bash

set -e

PIPELINE_VERSION=`cat /root/version.ini`
mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=fr.inria.repairnator:repairnator-pipeline:$PIPELINE_VERSION:jar:jar-with-dependencies -DremoteRepositories=ossSnapshot::::https://oss.sonatype.org/content/repositories/snapshots,oss::::https://oss.sonatype.org/content/repositories/releases -Ddest=/root/repairnator-pipeline.jar

echo "Pipeline version:"
java -jar /root/repairnator-pipeline.jar -h 2> /dev/null || true # We don't want this call to fail the script
echo "Repairnator-pipeline jar file installed in /root directory"
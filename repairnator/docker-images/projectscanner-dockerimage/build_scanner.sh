#!/bin/bash

set -e

mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=fr.inria.repairnator:repairnator-scanner:LATEST:jar:jar-with-dependencies -DremoteRepositories=ossSnapshot::::https://oss.sonatype.org/content/repositories/snapshots/ -Ddest=/root/repairnator-projectscanner.jar

echo "Scanner version:"
java -jar /root/scanner.jar -h 2> /dev/null || true # We don't want this call to fail the script
echo "scanner jar file installed in /root directory"
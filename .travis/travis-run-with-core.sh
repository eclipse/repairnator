#!/usr/bin/env bash
# running the Java tests of Repairnator on Travis

set -e
export M2_HOME=/usr/local/maven

sudo apt-get update
sudo apt-get install -y xmlstarlet

VERSION=`xmlstarlet sel -t -v '//_:project/_:properties/_:revision' src/pom.xml`
sed -i -e 's/\${revision}/'$VERSION'/' src/repairnator-core/pom.xml

mvn clean install -B -f src/repairnator-core/ && mvn -Dtest=TestPipelinebGithubMode clean test -B -f $TEST_PATH
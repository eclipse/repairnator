#!/bin/bash
# builds the maven-repair plugin

set -e
export M2_HOME=/usr/local/maven

VERSION=`xmlstarlet sel -t -v '//_:project/_:properties/_:revision' src/pom.xml`
sed -i -e 's/\${revision}/'$VERSION'/' src/repairnator-core/pom.xml
sed -i -e 's/\${revision}/'$VERSION'/' src/pom.xml
sed -i -e 's/\${revision}/'$VERSION'/' "${TEST_PATH}"/pom.xml

NPEFIX_VERSION=`xmlstarlet sel -t -v '//_:dependency[_:artifactId="npefix"]/_:version' src/maven-repair/pom.xml`

mvn clean install -B -f src/repairnator-core/ && mvn -Dtest=$TEST_LIST -DNPEFIX_VERSION=$NPEFIX_VERSION clean test -B -f $TEST_PATH -DskipTests
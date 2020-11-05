#!/bin/bash
# builds the maven-repair plugin

set -e
export M2_HOME=/usr/local/maven

sudo apt-get update
sudo apt-get install -y xmlstarlet

VERSION=`xmlstarlet sel -t -v '//_:project/_:properties/_:revision' src/pom.xml`
sed -i -e 's/\${revision}/'$VERSION'/' src/repairnator-core/pom.xml
NPEFIX_VERSION=`python3 -c 'import xml.etree.ElementTree; print(xml.etree.ElementTree.parse("src/maven-repair/pom.xml").findall(".//mvn:dependency[mvn:artifactId=\"npefix\"]/mvn:version",{"mvn":"http://maven.apache.org/POM/4.0.0"})[0].text)'`

mvn clean install -B -f src/repairnator-core/ && mvn -Dtest=$TEST_LIST -DNPEFIX_VERSION=$NPEFIX_VERSION clean test -B -f $TEST_PATH
#!/bin/bash
# builds the maven-repair plugin

set -e

cd src/maven-repair

NPEFIX_VERSION=`python3 -c 'import xml.etree.ElementTree; print(xml.etree.ElementTree.parse("pom.xml").findall("//mvn:dependency[mvn:artifactId=\"npefix\"]/mvn:version",{"mvn":"http://maven.apache.org/POM/4.0.0"})[0].text)'`

mvn test -DNPEFIX_VERSION=$NPEFIX_VERSION

cd ..

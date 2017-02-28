#!/bin/bash

cd /home/repairnator/github/nopol/nopol
git pull
mvn clean install

cd /home/repairnator/github/librepair
git pull

cd jtravis
mvn clean install

cd ../repairnator
mvn clean install

if [[ $? == 0 ]]
then
   cp -f ./target/repairnator-1.0-SNAPSHOT-jar-with-dependencies.jar /home/repairnator/scripts/repairnator.jar
else
   echo "Error while building a new version of repairnator"
fi

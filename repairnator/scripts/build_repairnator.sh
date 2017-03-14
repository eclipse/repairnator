#!/bin/bash

export M2_HOME=/opt/apache-maven-3.3.9
export PATH=$PATH:$M2_HOME/bin

if [ -z "$HOME_REPAIR" ]; then
    echo "The variable HOME_REPAIR must be set."
    exit -1
fi

cd $HOME_REPAIR/github/nopol/nopol
git pull
mvn clean install -DskipTests=true

cd $HOME_REPAIR/github/librepair
git pull

cd jtravis
mvn clean install -DskipTests=true

cd ../repairnator
mvn clean install -DskipTests=true

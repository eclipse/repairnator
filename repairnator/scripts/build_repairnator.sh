#!/bin/bash

if [ -z "$HOME_REPAIR" ]; then
    echo "The variable HOME_REPAIR must be set."
    exit -1
fi

cd $HOME_REPAIR/github/librepair
git pull

cd jtravis
mvn clean install -DskipTests=true

cd ../repairnator
mvn clean install -DskipTests=true

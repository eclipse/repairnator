#!/bin/bash

if [ -z "$REPAIRNATOR_GH_REPO_PATH" ]; then
    echo "The variable REPAIRNATOR_GH_REPO_PATH must be set."
    exit -1
fi

cd $REPAIRNATOR_GH_REPO_PATH
mvn clean install -DskipTests=true

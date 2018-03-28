#!/bin/bash
set -e

if [ -z "$REPAIRNATOR_GH_REPO_PATH" ]; then
    echo "The variable REPAIRNATOR_GH_REPO_PATH must be set."
    exit -1
fi

if [ ! -d "$REPAIRNATOR_ROOT_CLONE" ]; then
    mkdir -p $REPAIRNATOR_ROOT_CLONE
    cd $REPAIRNATOR_ROOT_CLONE
    git clone --depth=1 https://github.com/Spirals-Team/repairnator.git
fi

cd $REPAIRNATOR_GH_REPO_PATH
mvn clean install -DskipTests=true

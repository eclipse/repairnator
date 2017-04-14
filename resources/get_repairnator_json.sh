#!/usr/bin/env bash

if [ "$#" -ne 2 ]; then
    echo "Usage: ./get_repairnator-json.sh <github repository> <destination>"
    exit -1
fi

TMP_GIT_DIR=/tmp/getjson_`uuidgen`
DEST=$2
REPO=$1

if [ ! -d "$DEST" ]; then
    echo "Destination path should exist"
    exit -1
fi

mkdir $TMP_GIT_DIR
cd $TMP_GIT_DIR
git clone $REPO $TMP_GIT_DIR

if [[ $? != 0 ]]
then
    echo "Error while cloning"
    exit 1
fi

git for-each-ref --shell --format="branchname=%(refname:strip=3)" refs/remotes | \
while read entry
do
    eval "$entry"
    if [ "$branchname" == "master" ]; then
        echo "Master branch ignored"
    elif [ "$branchname" == "HEAD" ]; then
        echo "Head ref ignored"
    else
        echo "Treating branch $branchname"
        mkdir $DEST/$branchname
        git checkout $branchname

        if [ -d "repairnator.json" ]; then
            echo "found repairnator.json"
            mkdir $DEST/$branchname
            cp -f repairnator.json $DEST/$branchname/
        elif [ -d "repairnator.properties" ]; then
            echo "Found repairnator.properties"
            mkdir $DEST/$branchname
            cp -f repairnator.properties $DEST/$branchname/
        else
            echo "No property or json file has been found for repairnator."
        fi
    fi
done

rm -rf $TMP_GIT_DIR
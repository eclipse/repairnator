#!/usr/bin/env bash

JSON_SCHEMA="`pwd`/repairnator-schema.json"

if [ "$#" -ne 2 ]; then
    echo "Usage: ./extract_buggybuildid.sh <github repository> <destination>"
    exit -1
fi

if [ ! -f $JSON_SCHEMA ]; then
    echo "The json schema ($JSON_SCHEMA) cannot be found."
    exit -1
fi

command -v ajv >/dev/null 2>&1 || { echo >&2 "I require ajv (https://github.com/jessedc/ajv-cli) but it's not installed.  Aborting."; exit 1; }
command -v jq >/dev/null 2>&1 || { echo >&2 "I require jq but it's not installed.  Aborting."; exit 1; }

DEST=$2
REPO=$1

if [ -f "$DEST" ]; then
    echo "Destination path should not exist"
    exit -1
fi

if [ ! -d "$1" ]; then
    TMP_GIT_DIR=/tmp/extractbuggybuildid_`uuidgen`
    REPO=$1
    mkdir $TMP_GIT_DIR
    cd $TMP_GIT_DIR
    git clone $REPO $TMP_GIT_DIR
    if [[ $? != 0 ]]
    then
        echo "Error while cloning"
        exit 1
    fi
else
    TMP_GIT_DIR=$1
    cd $TMP_GIT_DIR
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
        git checkout $branchname

        if [ -e "repairnator.json" ]; then
            echo "found repairnator.json in $branchname"
            if ajv test -s $JSON_SCHEMA -d repairnator.json --valid ; then
                BUILDID=`jq -r .metrics.BuggyBuildId repairnator.json`
                echo "repairnator.json is valid in $branchname. Found following buildid: $BUILDID"
                echo "$BUILDID $branchname" >> $DEST
            else
                BUILDID=`jq -r .buildid repairnator.json`
                if [ "$BUILDID" != "null" ]; then
                    echo "repairnator.json is NOT valid in $branchname but Found following buildid: $BUILDID"
                    echo "$BUILDID $branchname" >> $DEST
                else
                    echo "repairnator.json is not valid and no interesting value found in $branchname"
                fi
            fi
        else
            echo "No property or json file has been found for repairnator in $branchname."
        fi
    fi
done
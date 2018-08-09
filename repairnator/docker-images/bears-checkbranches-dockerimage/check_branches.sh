#!/usr/bin/env bash

if [ "$#" -ne 2 ]; then
    echo "Usage: ./check_branches.sh <github repository> <branch name>"
    exit 2
fi


DOCKER_DEST=/tmp/result.txt
REPO=$1
BRANCH_NAME=$2

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

command -v ajv >/dev/null 2>&1 || { echo >&2 "I require ajv (https://github.com/jessedc/ajv-cli) but it's not installed.  Aborting."; exit 1; }

SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"
JSON_SCHEMA="$SCRIPT_DIR/bears-schema.json"
echo "$JSON_SCHEMA"
if [ ! -f $JSON_SCHEMA ]; then
    echo "The json schema ($JSON_SCHEMA) cannot be found."
    exit -1
fi

MAVEN_TEST_ARGS="-Denforcer.skip=true -Dcheckstyle.skip=true -Dcobertura.skip=true -DskipITs=true -Drat.skip=true -Dlicense.skip=true -Dfindbugs.skip=true -Dgpg.skip=true -Dskip.npm=true -Dskip.gulp=true -Dskip.bower=true"

echo "Treating branch $BRANCH_NAME"
mkdir repo && cd repo && git init
git remote add origin $REPO
git fetch origin $BRANCH_NAME
git checkout $BRANCH_NAME

if [ -e "bears.json" ]; then
    if ajv test -s ../$JSON_SCHEMA -d bears.json --valid ; then
        echo "bears.json is valid in $BRANCH_NAME"
    else
        RESULT="$BRANCH_NAME [FAILURE] (bears.json is invalid)"
        >&2 echo -e "$RED $RESULT $NC"
        echo "$RESULT" >> $DOCKER_DEST
        exit 2
    fi
else
    RESULT="$BRANCH_NAME [FAILURE] (bears.json does not exist)"
    >&2 echo -e "$RED $RESULT $NC"
    echo "$RESULT" >> $DOCKER_DEST
    exit 2
fi

numberOfCommits=`git rev-list --count HEAD`

if [ "$numberOfCommits" -ne 3 ] && [ "$numberOfCommits" -ne 4 ]; then
    RESULT="$BRANCH_NAME [FAILURE] (the number of commits is different than 3 and 4)"
    >&2 echo -e "$RED $RESULT $NC"
    echo "$RESULT" >> $DOCKER_DEST
    exit 2
fi

bugCommitId=`git log --format=format:%H --grep="Bug commit"`
patchCommitId=`git log --format=format:%H --grep="Human patch"`

if [ "$numberOfCommits" -eq 4 ]; then
    bugCommitId=`git log --format=format:%H --grep="Changes in the tests"`
fi

echo "Checking out the bug commit: $bugCommitId"
git log --format=%B -n 1 $bugCommitId

git checkout -q $bugCommitId

timeout 1800s mvn -q -B test -Dsurefire.printSummary=false $MAVEN_TEST_ARGS

status=$?
if [ "$status" -eq 0 ]; then
    >&2 echo -e "$RED Error while reproducing the bug for branch $BRANCH_NAME $NC (status = $status)"
    echo "$BRANCH_NAME [FAILURE] (bug reproduction - status = $status)" >> $DOCKER_DEST
    exit 2
elif [ "$status" -eq 124 ]; then
    >&2 echo -e "$RED Error while reproducing the bug for branch $BRANCH_NAME $NC"
    echo "$BRANCH_NAME [FAILURE] (bug reproduction timeout)" >> $DOCKER_DEST
    exit 2
fi

echo "Checking out the patch commit: $patchCommitId"
git log --format=%B -n 1 $patchCommitId

git checkout -q $patchCommitId

timeout 1800s mvn -q -B test -Dsurefire.printSummary=false $MAVEN_TEST_ARGS

status=$?
if [ "$status" -eq 124 ]; then
    >&2 echo -e "$RED Error while reproducing the passing build for branch $BRANCH_NAME $NC"
    echo "$BRANCH_NAME [FAILURE] (patch reproduction timeout)" >> $DOCKER_DEST
    exit 2
elif [ "$status" -ne 0 ]; then
    >&2 echo -e "$RED Error while reproducing the passing build for branch $BRANCH_NAME $NC (status = $status)"
    echo "$BRANCH_NAME [FAILURE] (patch reproduction - status = $status)" >> $DOCKER_DEST
    exit 2
fi

RESULT="$BRANCH_NAME [OK]"
echo -e "$GREEN $RESULT $NC"
echo "$RESULT" >> $DOCKER_DEST
#!/usr/bin/env bash

if [ "$#" -ne 2 ]; then
    echo "Usage: ./check_branches.sh <github repository> <destination path>"
    exit 2
fi

TMP_GIT_DIR=/tmp/checkbranches_`uuidgen`
REPO=$1
DEST=$2

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

mkdir $TMP_GIT_DIR
cd $TMP_GIT_DIR
git clone $REPO $TMP_GIT_DIR

if [[ $? != 0 ]]
then
    echo "Error while cloning"
    exit 1
fi

echo "Analyze started: `date "+%Y-%m-%d_%H%M%S"`" > $DEST
echo "Considered repository: $REPO" > $DEST

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
        git checkout -q $branchname

        bugCommitId=`git log --format=format:%H --grep="Bug commit"`
        echo "Checking out the bug commit $bugCommitId"
        git log --format=%B -n 1 $bugCommitId

        git checkout -q $bugCommitId
        mvn -q install -DskipTests=true
        if [ "$?" -ne 0 ]; then
            >&2 echo -e "$RED Error while building the project with bug for branch $branchname $NC"
            echo "$branchname [FAILURE] (bug build)" >> $DEST
            continue
        fi

        mvn -q -Dsurefire.printSummary=false test

        if [ "$?" -eq 0 ]; then
            >&2 echo -e "$RED Error while reproducing the bug for branch $branchname $NC"
            echo "$branchname [FAILURE] (bug reproduction)" >> $DEST
            continue
        fi

        patchCommitId=`git log --format=format:%H --grep="Human patch"`
        echo "Checking out the patch commit $patchCommitId"
        git log --format=%B -n 1 $patchCommitId

        git checkout -q $patchCommitId
        mvn -q clean install -DskipTests=true
        if [ "$?" -ne 0 ]; then
            >&2 echo -e "$RED Error while building the project with patch for branch $branchname $NC"
            echo "$branchname [FAILURE] (patch build)" >> $DEST
            continue
        fi

        mvn -q -Dsurefire.printSummary=false test

        if [ "$?" -ne 0 ]; then
            >&2 echo -e "$RED Error while reproducing the passing build for branch $branchname $NC"
            echo "$branchname [FAILURE] (patch reproduction)" >> $DEST
            continue
        fi

        echo -e "$GREEN Branch $branchname OK $NC"
        echo "$branchname [OK]" >> $DEST
    fi
done

echo "Analyze finished: `date "+%Y-%m-%d_%H%M%S"`" >> $DEST

rm -rf $TMP_GIT_DIR

echo "All results can be found in $DEST"
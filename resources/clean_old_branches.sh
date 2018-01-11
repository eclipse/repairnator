#!/usr/bin/env bash

if [ "$#" -ne 1 ]; then
    echo "Usage: ./clean_old_branches.sh <Github repository>"
    exit -1
fi

TMP_FILE_OLDBRANCHES=/tmp/oldbranches_`uuidgen`
TMP_FILE_COUNTER=/tmp/counter_`uuidgen`
TMP_GIT_DIR=/tmp/clean_repo_`uuidgen`
REPO=$1

mkdir $TMP_GIT_DIR
cd $TMP_GIT_DIR
git clone $REPO $TMP_GIT_DIR

if [[ $? != 0 ]]
then
    echo "Error while cloning"
    exit 1
fi

git for-each-ref --shell --format="branchname=%(refname:strip=3)" refs/remotes | \
if [[ 1 -eq 1 ]]; then
    while read entry
    do
        eval "$entry"
        if [ "$branchname" == "master" ]; then
            echo "Master branch ignored"
        elif [ "$branchname" == "HEAD" ]; then
            echo "Head ref ignored"
        elif [[ "$branchname" == "surli-failingProject"* ]]; then
            echo "Branch detected from surli/failingProject: $branchname"
            export OLD_BRANCHES="$OLD_BRANCHES $branchname"
            export COUNTER=$((COUNTER+1))
        else
            echo "Treating branch $branchname"
            git checkout $branchname

            if [ -e "repairnator.json" ]; then
                echo "found repairnator.json"
            else
                echo "No repairnator.json found. The branch is marked to be deleted."
                export OLD_BRANCHES="$OLD_BRANCHES $branchname"
                export COUNTER=$((COUNTER+1))
            fi
        fi
    done

    echo $COUNTER > $TMP_FILE_COUNTER
    echo $OLD_BRANCHES > $TMP_FILE_OLDBRANCHES
fi

COUNTER=$(cat $TMP_FILE_COUNTER)
OLD_BRANCHES=$(cat $TMP_FILE_OLDBRANCHES)

while true; do
        read -p "Do you wish to delete the following $COUNTER branches: $OLD_BRANCHES?" yn
        case $yn in
            [Yy]* ) git push origin --delete $OLD_BRANCHES; break;;
            [Nn]* ) exit;;
            * ) echo "Please answer yes or no.";;
        esac
    done

rm $TMP_FILE_OLDBRANCHES
rm $TMP_FILE_COUNTER
rm -rf $TMP_GIT_DIR
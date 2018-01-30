#!/usr/bin/env bash

if [ "$#" -ne 1 ]; then
    echo "Usage: ./check_commit_branch.sh <Github repository>"
    exit -1
fi

TMP_FILE_OLDBRANCHES=/tmp/oldbranches_`uuidgen`
TMP_FILE_COUNTER=/tmp/counter_`uuidgen`

if [ ! -d "$1" ]; then
    TMP_GIT_DIR=/tmp/clean_repo_`uuidgen`
    REPO=$1
    mkdir $TMP_GIT_DIR
    cd $TMP_GIT_DIR
    git clone $REPO $TMP_GIT_DIR
else
    TMP_GIT_DIR=$1
    cd $TMP_GIT_DIR
fi



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
        else
            echo "Treating branch $branchname"
            git checkout $branchname

            NB_COMMIT=`git rev-list --count HEAD`

            if [ $NB_COMMIT != "3" ] && [ $NB_COMMIT != "4" ]; then
                echo "The number of commit is not 3 or 4 but : $NB_COMMIT"
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
echo "The following $COUNTER branches have a wrong number of commits: $OLD_BRANCHES"
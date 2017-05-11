#!/usr/bin/env bash

if [ "$#" -lt 2 ]; then
    echo "Usage: ./check_branches.sh <github repository> <destination path> [list of branches path]"
    exit 2
fi

if [ "$#" -eq 3 ]; then
    USE_FILE=1
    BRANCH_FILE=$3
else
    USE_FILE=0
fi

TMP_GIT_DIR=/tmp/checkbranches
DOCKER_DEST=/tmp/result.txt
REPO=$1
DEST=$2

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

MAVEN_TEST_ARGS="-Denforcer.skip=true -Dcheckstyle.skip=true -Dcobertura.skip=true -DskipITs=true -Drat.skip=true -Dlicense.skip=true"


function check_branch() {
    branchname=$1
    echo "Treating branch $branchname"

    docker run -i -v $DEST:$DOCKER_DEST --rm=true maven:3.3.9-jdk-8 bash -s - << EOF
        mkdir repo && cd repo && git init
        git remote add origin $REPO
        git fetch origin $branchname
        git checkout $branchname

        bugCommitId=\`git log --format=format:%H --grep="Bug commit"\`
        patchCommitId=\`git log --format=format:%H --grep="Human patch"\`

        echo "Checking out the bug commit: \$bugCommitId"
        git log --format=%B -n 1 \$bugCommitId

        git checkout -q \$bugCommitId

        timeout 1800s mvn -q -B test -Dsurefire.printSummary=false $MAVEN_TEST_ARGS

        status=\$?
        if [ "\$status" -eq 0 ]; then
            >&2 echo -e "$RED Error while reproducing the bug for branch $branchname $NC (status = \$status)"
            echo "$branchname [FAILURE] (bug reproduction - status = \$status)" >> $DOCKER_DEST
            exit 2
        elif [ "\$status" -eq 124 ]; then
            >&2 echo -e "$RED Error while reproducing the bug for branch $branchname $NC"
            echo "$branchname [FAILURE] (bug reproduction timeout)" >> $DOCKER_DEST
            exit 2
        fi

        echo "Checking out the patch commit: \$patchCommitId"
        git log --format=%B -n 1 \$patchCommitId

        git checkout -q \$patchCommitId

        timeout 1800s mvn -q -B test -Dsurefire.printSummary=false $MAVEN_TEST_ARGS

        status=\$?
        if [ "\$status" -eq 124 ]; then
            >&2 echo -e "$RED Error while reproducing the passing build for branch $branchname $NC"
            echo "$branchname [FAILURE] (patch reproduction timeout)" >> $DOCKER_DEST
            exit 2
        elif [ "\$status" -ne 0 ]; then
            >&2 echo -e "$RED Error while reproducing the passing build for branch $branchname $NC (status = \$status)"
            echo "$branchname [FAILURE] (patch reproduction - status = \$status)" >> $DOCKER_DEST
            exit 2
        fi

        echo -e "$GREEN Branch $branchname OK $NC"
        echo "$branchname [OK]" >> $DOCKER_DEST
EOF
}

echo "Analyze started: `date "+%Y-%m-%d_%H%M%S"`" > $DEST
echo "Considered repository: $REPO" >> $DEST

if [ $USE_FILE -eq 0 ]; then
    mkdir $TMP_GIT_DIR
    cd $TMP_GIT_DIR
    git clone $REPO $TMP_GIT_DIR

    if [[ $? != 0 ]]
    then
        echo "Error while cloning"
        exit 1
    fi
    git for-each-ref --shell --format="branchname=%(refname:short)" refs/remotes | \
    while read entry
    do
        eval "$entry"
        if [ "$branchname" == "origin/master" ]; then
            echo "Master branch ignored"
        elif [ "$branchname" == "origin/HEAD" ]; then
            echo "Head ref ignored"
        else
            check_branch $branchname
        fi
    done
else
    while IFS='' read -r line || [[ -n "$line" ]]; do
        check_branch $line
    done < "$BRANCH_FILE"
fi

echo "Analyze finished: `date "+%Y-%m-%d_%H%M%S"`" >> $DEST

rm -rf $TMP_GIT_DIR

echo "All results can be found in $DEST"
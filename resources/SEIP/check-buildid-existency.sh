#!/usr/bin/env bash

#CSV_FILE="seip-reproduced-bugs.csv"
#CSV_FILE="seip-reproduced-bugs-nobranch.csv"
#LIST_BRANCHES="librepair-xp-remote-branches.txt"

CSV_FILE="missing-branches.txt"
LIST_BRANCHES="../buildids-associated-branches.txt"

OUTPUT="missing-branches-2.txt"
RETRIEVED_BRANCHES="retrieved-branches-2.txt"

if [ ! -f $CSV_FILE ] || [ ! -f $LIST_BRANCHES ]; then
    echo "This script must be executed in the same directory as $CSV_FILE and $LIST_BRANCHES"
    exit -1
fi

while read buildId repoName; do
    #repoClean=${repoName//\//-}
    if grep --quiet "$buildId" $LIST_BRANCHES; then
        echo "$buildId $repoName" >> $RETRIEVED_BRANCHES
    else
        echo "$buildId $repoName" >> $OUTPUT
    fi
done < $CSV_FILE
#!/usr/bin/env bash

set -e

if [ "$#" -ne 3 ]; then
    echo "Usage: ./diff_repo_for_replication [original remote repo] [target remote repo] [dest path]"
    exit -1
fi

tempdir=/tmp/diff_repair_`date "+%Y-%m-%d_%H%M%S"`
originRepoDir=$tempdir/originRepo
targetRepoDir=$tempdir/targetRepo

originalBuildIds=$tempdir/original_build_ids.txt
targetBuildIds=$tempdir/target_build_ids.txt

finalList=$tempdir/final_list.txt

branch_regex="^.*-([0-9]{9})(-[0-9]{8}-[0-9]{6})?.*[)]$"

list_branch_repo=$tempdir/list_branch_repo.txt

mkdir $tempdir
mkdir $originRepoDir
mkdir $targetRepoDir

cd $originRepoDir

git init
git remote add origin $1
git remote show origin > $list_branch_repo

echo "Process branches..."
sed -E "s/^.*-([0-9]{9})(-[0-9]{8}-[0-9]{6})?.*[)]$/\1/g" < $list_branch_repo | grep -o "^[0-9]*$" | sort > $originalBuildIds

cd $targetRepoDir

git init
git remote add origin $2
git remote show origin > $list_branch_repo

sed -E "s/^.*-([0-9]{9})(-[0-9]{8}-[0-9]{6})?.*[)]$/\1/g" < $list_branch_repo | grep -o "^[0-9]*$" | sort > $targetBuildIds

comm -3 -2 $originalBuildIds $targetBuildIds > $3

#rm -rf $tempdir
#!/bin/bash

set -e

# inspired from stackoverflow answer: http://stackoverflow.com/questions/11929766/how-to-delete-all-git-commits-except-the-last-five and http://stackoverflow.com/questions/7005513/pass-an-argument-to-a-git-alias-command
git config --global alias.rebase-last-x '!b="$(git branch --no-color | grep "*" |  cut -c3-)" && h="$(git rev-parse $b)" && echo "Current branch: $b $h" && c="$(git rev-parse $b~$(($1-1)))" && echo "Recreating $b branch with initial commit $c ..." && git checkout --orphan new-start $c && git commit -C $c && git rebase --onto new-start $c $b && git branch -d new-start && git gc && echo "Done."'
git config --global core.whitespace trailing-space,space-before-tab
git config --global apply.whitespace fix

git config --global user.name "Luc Esape"
git config --global user.email "luc.esape@gmail.com"

echo "You can now use the command 'git rebase-last-x 10' to rebase a repo keeping 10 commits"

cd /root
mkdir github
cd github
git clone https://github.com/Spirals-Team/librepair.git

echo "LibRepair repository cloned."

cp librepair/travis/travis-install.sh .
chmod +x travis-install.sh

./travis-install.sh

echo "All dependencies installed for repairnator."

cd librepair/jtravis
mvn clean install -DskipTests=true

echo "JTravis compiled and installed"

cd ../repairnator
mvn clean install -DskipTests=true

echo "Repairnator compiled and installed"

cp repairnator-pipeline/target/repairnator-pipeline-*-with-dependencies.jar /root/repairnator-pipeline.jar

echo "Repairnator-pipeline jar file installed in /root directory"
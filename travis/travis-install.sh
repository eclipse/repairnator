#!/usr/bin/env bash

cd scripts
chmod +x install_git_rebase_last.sh
./install_git_rebase_last.sh

if [[ $? != 0 ]]
then
    echo "Error while installing git aliases"
    exit 1
fi

git clone https://github.com/SpoonLabs/CoCoSpoon.git
cd CoCoSpoon
mvn clean install

if [[ $? != 0 ]]
then
    echo "Error while installing CoCoSpoon"
    exit 1
fi

cd ..

git clone https://github.com/SpoonLabs/nopol.git
cd nopol/nopol

mvn clean install -DskipTests=true

if [[ $? != 0 ]]
then
    echo "Error while installing nopol"
    exit 1
fi

cd ../..

git clone https://github.com/surli/maven-surefire.git
cd maven-surefire/surefire-report-parser

mvn clean install

if [[ $? != 0 ]]
then
    echo "Error while installing surefire-report-parser"
    exit 1
fi
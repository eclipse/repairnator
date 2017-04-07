#!/usr/bin/env bash

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
cd maven-surefire
git checkout surefire-parser-feature-error-status
cd surefire-report-parser

mvn clean install

if [[ $? != 0 ]]
then
    echo "Error while installing surefire-report-parser"
    exit 1
fi
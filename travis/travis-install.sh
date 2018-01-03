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

git clone https://github.com/apache/maven-surefire.git
cd maven-surefire
git checkout SUREFIRE-1416
cd surefire-report-parser

mvn clean install

if [[ $? != 0 ]]
then
    echo "Error while installing surefire-report-parser"
    exit 1
fi

cd ../..
git clone https://github.com/Spirals-Team/npefix-maven
cd npefix-maven
mvn install

if [[ $? != 0 ]]
then
    echo "Error while installing NPEfix"
    exit 1
fi

cd ../..
git clone https://github.com/SpoonLabs/astor.git
cd astor

mvn clean
mvn install -DskipTests=true

if [[ $? != 0 ]]
then
    echo "Error while installing Astor"
    exit 1
fi
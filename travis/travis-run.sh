#!/usr/bin/env bash

cd jtravis
mvn clean install jacoco:report coveralls:report

if [[ $? != 0 ]]
then
    echo "Error while installing jTravis"
    exit 1
fi

cd ..

git config --global user.name "Luc Esape"
git config --global user.email "luc.esape@gmail.com"

cd repairnator

chmod +x ./dockerImage/configure_git.sh
./dockerImage/configure_git.sh

if [[ $? != 0 ]]
then
    echo "Error while installing git aliases"
    exit 1
fi

mvn clean install

if [[ $? != 0 ]]
then
    echo "Error while installing repairnator"
    exit 1
fi

mvn jacoco:report coveralls:report --fail-never

cd ..

cd travisFilter
mvn clean install

if [[ $? != 0 ]]
then
    echo "Error while installing travisFilter"
    exit 1
fi
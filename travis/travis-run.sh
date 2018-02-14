#!/usr/bin/env bash

git config --global user.name "Luc Esape"
git config --global user.email "luc.esape@gmail.com"

export M2_HOME=/usr/local/maven

cd repairnator

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
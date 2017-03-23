#!/usr/bin/env bash

cd jtravis
mvn clean install

if [[ $? != 0 ]]
then
    echo "Error while installing jTravis"
    exit 1
fi

cd ..

cd repairnator
mvn clean install
mvn jacoco:report coveralls:report --fail-never

if [[ $? != 0 ]]
then
    echo "Error while installing repairnator"
    exit 1
fi

cd ..

cd travisFilter
mvn clean install

if [[ $? != 0 ]]
then
    echo "Error while installing travisFilter"
    exit 1
fi
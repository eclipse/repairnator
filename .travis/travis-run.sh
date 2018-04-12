#!/usr/bin/env bash

set -e

git config --global user.name "Luc Esape"
git config --global user.email "luc.esape@gmail.com"

export M2_HOME=/usr/local/maven

cd repairnator
mvn clean install
mvn jacoco:report coveralls:report --fail-never


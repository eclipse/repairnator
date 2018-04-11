#!/usr/bin/env bash

if [ "$TRAVIS_PULL_REQUEST" = "false" ] && [ "$TRAVIS_BRANCH" = "master" ]; then
    cp .travis/travis-settings.xml $HOME/.m2/settings.xml && cd repairnator && mvn deploy -DskipTests
else
    echo "Nothing to deploy when on PR or other branch"
fi
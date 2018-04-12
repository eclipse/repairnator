#!/usr/bin/env bash
set -e

if [ "$TRAVIS_PULL_REQUEST" = "false" ] && [ "$TRAVIS_BRANCH" = "master" ]; then
    cp .travis/travis-settings.xml $HOME/.m2/settings.xml
    cd repairnator
    mvn deploy -DskipTests
    echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
    if [ "$TRAVIS_TAG" = "$TRAVIS_BRANCH" ]; then
        TAG=$TRAVIS_TAG
    else
        TAG="latest"
    fi

    docker build -f docker-images/pipeline-dockerimage -t surli/repairnator:$TAG
    docker push surli/repairnator:$TAG
else
    echo "Nothing to deploy when on PR or other branch"
fi
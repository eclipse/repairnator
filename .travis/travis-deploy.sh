#!/usr/bin/env bash
set -e

if [ "$TRAVIS_PULL_REQUEST" = "false" ] && [ "$TRAVIS_BRANCH" = "master" ]; then
    cp .travis/travis-settings.xml $HOME/.m2/settings.xml
    cd repairnator
    mvn deploy -DskipTests
    echo "$DOCKER_PASSWORD" | docker login -u repairnator --password-stdin
    if [ "$TRAVIS_TAG" = "$TRAVIS_BRANCH" ]; then
        TAG=$TRAVIS_TAG
    else
        TAG="latest"
    fi

    docker build -q -t spirals/repairnator:$TAG docker-images/pipeline-dockerimage
    if [[ $? != 0 ]]
    then
        echo "Error while building pipeline docker image"
        exit 1
    fi

    docker push spirals/repairnator:$TAG
    if [[ $? != 0 ]]
    then
        echo "Error while pushing pipeline docker image"
        exit 1
    else
        echo "Docker image pushed: spirals/repairnator:$TAG"
    fi

    docker build -q -t spirals/bears:$TAG docker-images/bears-dockerimage
    if [[ $? != 0 ]]
    then
        echo "Error while building bears docker image"
        exit 1
    fi

    docker push spirals/bears:$TAG
    if [[ $? != 0 ]]
    then
        echo "Error while pushing bears docker image"
        exit 1
    else
        echo "Docker image pushed: spirals/bears:$TAG"
    fi
else
    echo "Nothing to deploy when on PR or other branch"
fi
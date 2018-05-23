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

    docker build -q -t surli/repairnator:$TAG docker-images/pipeline-dockerimage
    if [[ $? != 0 ]]
    then
        echo "Error while building docker image"
        exit 1
    fi

    docker push surli/repairnator:$TAG
    if [[ $? != 0 ]]
    then
        echo "Error while pushing docker image"
        exit 1
    else
        echo "Docker image pushed: surli/repairnator:$TAG"
    fi
else
    echo "Nothing to deploy when on PR or other branch"
fi
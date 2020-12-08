#!/usr/bin/env bash
# CD for repairnator
# Continuous delivery & Continuous deployment

# we stop at the first failure
set -e

sudo apt-get update

### MAVEN CENTRAL
if [ "$TRAVIS_PULL_REQUEST" = "false" ] && [ "$TRAVIS_BRANCH" = "master" ]; then
    cp .travis/travis-settings.xml $HOME/.m2/settings.xml
    cd src 
    
    # pushing snapshot to https://oss.sonatype.org/content/repositories/snapshots/fr/inria/repairnator/
    mvn deploy -DskipTests -Dcheckstyle.skip
    echo Deployment to Maven Central done
fi


### DOCKERHUB
if [ "$TRAVIS_PULL_REQUEST" = "false" ] && [ "$TRAVIS_BRANCH" = "master" ]; then
    echo deploy to Dockerhub

    # $DOCKER_PASSWORD is set in the Travis UI
    echo "$DOCKER_PASSWORD" | docker login -u monperrus --password-stdin
    TAG="latest"

    # building the image
    docker build -q -t repairnator/pipeline:$TAG docker-images/pipeline-dockerimage
    if [[ $? != 0 ]]
    then
        echo "Error while building pipeline docker image"
        exit 1
    fi
    # pushing to dockerhub 
    docker push repairnator/pipeline:$TAG
    if [[ $? != 0 ]]
    then
        echo "Error while pushing pipeline docker image"
        exit 1
    else
        echo "Docker image pushed to Dockerhub"
    fi
fi

## CONTINUOUS DEPLOYMENT
# The following script udpates and restarts the known instances of Repairnator
curl -s https://raw.githubusercontent.com/repairnator/repairnator-cd/master/repairnator-cd.sh | bash





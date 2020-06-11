#!/usr/bin/env bash
set -e

sudo apt-get update
sudo apt-get install -y xmlstarlet

# install the latest kubectl
sudo curl -LO https://storage.googleapis.com/kubernetes-release/release/`curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt`/bin/linux/amd64/kubectl \
  && sudo chmod +x ./kubectl \
  && sudo mv ./kubectl /usr/local/bin/

### MAVEN CENTRAL
if [ "$TRAVIS_PULL_REQUEST" = "false" ] && [ "$TRAVIS_BRANCH" = "master" ]; then
    cp .travis/travis-settings.xml $HOME/.m2/settings.xml
    cd src 
    
    # pushing snapshot to https://oss.sonatype.org/content/repositories/snapshots/fr/inria/repairnator/
    VERSION=`xmlstarlet sel -t -v '//_:project/_:properties/_:revision' pom.xml`
    sed -i -e 's/\${revision}/'$VERSION'/' pom.xml */pom.xml
    git diff
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
        echo "Docker image pushed"

        ### KUBERNETES
        echo "Restart the repairnator pipelines hosted in the k8s"
        # back to the root folder of the project
        cd ..
        # build the .kube directory and setup the config
        mkdir ${HOME}/.kube
        cp .travis/kubeconfig.skeleton ${HOME}/.kube/config
        sed -i 's/K8S_CERTIFICATE_AUTHORITY_DATA/'"$K8S_CERTIFICATE_AUTHORITY_DATA"'/g' ${HOME}/.kube/config
        sed -i 's/K8S_CLIENT_KEY_DATA/'"$K8S_CLIENT_KEY_DATA"'/g' ${HOME}/.kube/config
        sed -i 's/K8S_TOKEN/'"$K8S_TOKEN"'/g' ${HOME}/.kube/config
        # remove the current pipeline pods, k8s will automatically recreate them using the latest image
        timeout 20s kubectl delete pod $(kubectl get pods -n repairnator -l app=repairnator-pipeline -o custom-columns=NAME:.metadata.name --no-headers=true) -n repairnator || true
    fi
fi





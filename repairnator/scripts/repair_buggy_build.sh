#!/usr/bin/env bash
set -e

function usage {
    echo "This script aims at launching repairnator on a given TravisCI build id"
    echo "Usage: repair_buggy_build.sh [-d] <build_id>"
    exit -1
}

function ca {
  if [ -z "$2" ];
  then
      echo ""
  else
    if [ "$2" == "null" ];
    then
        echo ""
    else
        echo "$1=$2 "
    fi
  fi
}

if [ "$#" -lt 1 ]; then
    usage
fi

DAEMON_MODE=0

re='^[0-9]+$'
if [ "$1" == "-d" ]; then
    DAEMON_MODE=1
    BUILD_ID=$2
else
    BUILD_ID=$1
fi


if ! [[ $BUILD_ID =~ $re ]]; then
    echo "Build id should be a number"
    usage
fi

SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

. $SCRIPT_DIR/utils/init_script.sh

echo "Pull the docker machine (name: $DOCKER_TAG)..."
docker pull $DOCKER_TAG

LOG_FILENAME="repairnator-pipeline_`date \"+%Y-%m-%d_%H%M\"`_$BUILD_ID"

DOCKER_ARGS="--env REPAIR_MODE=repair"
DOCKER_ARGS="$DOCKER_ARGS --env BUILD_ID=$BUILD_ID"
DOCKER_ARGS="$DOCKER_ARGS --env GITHUB_OAUTH=$GITHUB_OAUTH"
DOCKER_ARGS="$DOCKER_ARGS --env LOG_FILENAME=$LOG_FILENAME"
DOCKER_ARGS="$DOCKER_ARGS --env RUN_ID=$RUN_ID"
DOCKER_ARGS="$DOCKER_ARGS --env OUTPUT=/var/log"
DOCKER_ARGS="$DOCKER_ARGS --env REPAIR_TOOLS=$REPAIR_TOOLS"

DOCKER_ARGS="$DOCKER_ARGS `ca \"--env PUSH_URL\" $PUSH_URL`"
DOCKER_ARGS="$DOCKER_ARGS `ca \"--env GITHUB_USERNAME\" $GITHUB_USERNAME`"
DOCKER_ARGS="$DOCKER_ARGS `ca \"--env GITHUB_USEREMAIL\" $GITHUB_USEREMAIL`"
DOCKER_ARGS="$DOCKER_ARGS `ca \"--env SMTP_SERVER\" $SMTP_SERVER`"
DOCKER_ARGS="$DOCKER_ARGS `ca \"--env NOTIFY_TO\" $NOTIFY_TO`"
DOCKER_ARGS="$DOCKER_ARGS `ca \"--env MONGODB_HOST\" $MONGODB_HOST`"
DOCKER_ARGS="$DOCKER_ARGS `ca \"--env MONGODB_NAME\" $MONGODB_NAME`"

DOCKER_COMMAND="docker run -d $DOCKER_ARGS -v $LOG_DIR:/var/log $DOCKER_TAG"
echo "Launch docker container with the following command: $DOCKER_COMMAND"

DOCKER_ID=`$DOCKER_COMMAND`

if [ "$DAEMON_MODE" -eq 1 ]; then
    echo "The container is launched with the following container id: $DOCKER_ID"
    echo "log command:"
    echo docker logs -f $DOCKER_ID
else
    docker logs -f $DOCKER_ID
fi

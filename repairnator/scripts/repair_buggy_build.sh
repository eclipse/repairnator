#!/usr/bin/env bash
set -e

function usage {
    echo "This script aims at launching repairnator on a given TravisCI build id"
    echo "Usage: repair_buggy_build.sh <build_id>"
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

if [ "$#" -ne 1 ]; then
    usage
fi

BUILD_ID=$1
SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

echo "Set environment variables"
source $SCRIPT_DIR/set_env_variable.sh

if [ -z "$HOME_REPAIR" ] || [ ! -d $HOME_REPAIR ]; then
    mkdir -p $HOME_REPAIR
fi

mkdir -p $ROOT_LOG_DIR
mkdir -p $ROOT_BIN_DIR
mkdir -p $ROOT_OUT_DIR

echo "Create output dir : $REPAIR_OUTPUT_PATH"
mkdir $REPAIR_OUTPUT_PATH

if [ -z "$RUN_ID_SUFFIX" ]; then
    RUN_ID=`uuidgen`
else
    RUN_ID=`uuidgen`_$RUN_ID_SUFFIX
fi

echo "This will be run with the following RUN_ID: $RUN_ID"

echo "Create log directory: $LOG_DIR"
mkdir $LOG_DIR

echo "Pull the docker machine (name: $DOCKER_TAG)..."
docker pull $DOCKER_TAG

LOG_FILENAME="repairnator-pipeline_`date \"+%Y-%m-%d_%H%M\"`_$BUILD_ID"
DOCKER_ARGS="--env BUILD_ID=$BUILD_ID"
DOCKER_ARGS="$DOCKER_ARGS --env GITHUB_OAUTH=$GITHUB_OAUTH"
DOCKER_ARGS="$DOCKER_ARGS --env LOG_FILENAME=$LOG_FILENAME"
DOCKER_ARGS="$DOCKER_ARGS --env RUN_ID=$RUN_ID"
DOCKER_ARGS="$DOCKER_ARGS --env OUTPUT=/var/log"
if [ "$BEARS_MDOE" -eq 1 ]; then
    DOCKER_ARGS="$DOCKER_ARGS --env REPAIR_MODE=bears"
else
    DOCKER_ARGS="$DOCKER_ARGS --env REPAIR_MODE=repair"
fi

DOCKER_ARGS="$DOCKER_ARGS `ca \"--env PUSH_URL\" $PUSH_URL`"
DOCKER_ARGS="$DOCKER_ARGS `ca \"--env SMTP_SERVER\" $SMTP_SERVER`"
DOCKER_ARGS="$DOCKER_ARGS `ca \"--env NOTIFY_TO\" $NOTIFY_TO`"
DOCKER_ARGS="$DOCKER_ARGS `ca \"--env MONGODB_HOST\" $MONGODB_HOST`"
DOCKER_ARGS="$DOCKER_ARGS `ca \"--env MONGODB_NAME\" $MONGODB_NAME`"


DOCKER_COMMAND="docker run -d $DOCKER_ARGS -v $LOG_DIR:/var/log $DOCKER_TAG"
echo "Launch docker container with the following command: $DOCKER_COMMAND"

$DOCKER_COMMAND

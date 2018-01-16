#!/usr/bin/env bash

#!/bin/bash
set -e

# Use to create args in the command line for optionnal arguments
function ca {
  if [ -z "$2" ];
  then
      echo ""
  else
    if [ "$2" == "null" ];
    then
        echo ""
    else
        echo "$1 $2 "
    fi
  fi
}

SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

echo "Set environment variables"
source $SCRIPT_DIR/set_env_variable.sh

mkdir $REPAIR_OUTPUT_PATH

if [ -z "$RUN_ID_SUFFIX" ]; then
    RUN_ID=`uuidgen`
else
    RUN_ID=`uuidgen`_$RUN_ID_SUFFIX
fi

echo "This will be run with the following RUN_ID: $RUN_ID"

echo "Create log directory: $LOG_DIR"
mkdir $LOG_DIR

echo "Start building a new version of repairnator"
$SCRIPT_DIR/build_repairnator.sh

if [[ $? != 0 ]]
then
   echo "Error while building a new version of repairnator"
   exit -1
fi

echo "Copy jars and prepare docker image"
mkdir $REPAIRNATOR_RUN_DIR
cp $REPAIRNATOR_REALTIME_JAR $REPAIRNATOR_REALTIME_DEST_JAR

echo "Pull the docker machine (name: $DOCKER_TAG)..."
docker pull $DOCKER_TAG

echo "Launch repairnator realtime scanner..."
args="`ca -s $GOOGLE_SECRET_PATH``ca --spreadsheet $SPREADSHEET``ca --dbhost $MONGODB_HOST``ca --dbname $MONGODB_NAME``ca --pushurl $PUSH_URL``ca --smtpServer $SMTP_SERVER``ca --notifyto $NOTIFY_TO``ca --jobsleeptime $JOB_SLEEP_TIME``ca --buildleeptime $BUILD_SLEEP_TIME``ca --maxinspectedbuilds $LIMIT_INSPECTED_BUILDS`"
if [ "$NOTIFY_ENDPROCESS" -eq 1 ]; then
    args="$args --notifyEndProcess"
fi

if [ "$CREATE_OUTPUT_DIR" -eq 1 ]; then
    args="$args --createOutputDir"
fi

echo "Supplementary args for realtime scanner $args"
java -jar $REPAIRNATOR_REALTIME_DEST_JAR -t $NB_THREADS -n $DOCKER_TAG -o $LOG_DIR -l $DOCKER_LOG_DIR -g $DAY_TIMEOUT --runId $RUN_ID $args &> $LOG_DIR/realtime.log
#!/bin/bash

SKIP_SCAN=0
if [ "$#" -eq 1 ]; then
    if [ ! -f $1 ]; then
        echo "The list of build id must be an existing file ($1 not found)"
        exit -1
    else
        SKIP_SCAN=1
    fi
fi

SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

echo "Set environment variables"
source $SCRIPT_DIR/set_env_variable.sh

if [ "$SKIP_SCAN" -eq 1 ]; then
    REPAIRNATOR_BUILD_LIST=$1
else
    mkdir $REPAIR_OUTPUT_PATH
fi

RUN_ID=`uuidgen`
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
cp $REPAIRNATOR_SCANNER_JAR $REPAIRNATOR_SCANNER_DEST_JAR
cp $REPAIRNATOR_DOCKERPOOL_JAR $REPAIRNATOR_DOCKERPOOL_DEST_JAR

mkdir $REPAIRNATOR_DOCKER_DIR
cp $REPAIRNATOR_PIPELINE_JAR $REPAIRNATOR_PIPELINE_DEST_JAR
cp -r $REPAIR_DOCKER_IMG_DIR/. $REPAIRNATOR_DOCKER_DIR

if [ "$SKIP_SCAN" -eq 0 ]; then
    echo "Start to scan projects for builds (dest file: $REPAIRNATOR_BUILD_LIST)..."
    java -jar $REPAIRNATOR_SCANNER_DEST_JAR -m $SCANNER_MODE -g $GOOGLE_SECRET_PATH -l $SCANNER_NB_HOURS -i $REPAIR_PROJECT_LIST_PATH -o $REPAIRNATOR_BUILD_LIST --runId $RUN_ID --dbhost $MONGODB_HOST --dbname $MONGODB_NAME -d &> $LOG_DIR/scanner.log
fi

NB_LINES=`wc -l $REPAIRNATOR_BUILD_LIST`

if [[ $NB_LINES == 0 ]]
then
   echo "No build has been found. Stop the script now."
   exit 0
fi

echo "Build the docker machine (tag: $DOCKER_TAG)..."
docker build -t $DOCKER_TAG $REPAIRNATOR_DOCKER_DIR

echo "Launch docker pool..."
java -jar $REPAIRNATOR_DOCKERPOOL_DEST_JAR -t $NB_THREADS -n $DOCKER_TAG -i $REPAIRNATOR_BUILD_LIST -l $LOG_DIR -g $DAY_TIMEOUT -s $GOOGLE_SECRET_PATH --runId $RUN_ID --dbhost $MONGODB_HOST --dbname $MONGODB_NAME &> $LOG_DIR/dockerpool.log

echo "Docker pool finished, delete the run directory ($REPAIRNATOR_RUN_DIR)"
rm -rf $REPAIRNATOR_RUN_DIR
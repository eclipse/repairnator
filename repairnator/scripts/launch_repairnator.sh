#!/bin/bash

echo "Set environment variables"
./set_env_variable.sh

DOCKER_VERSION=`date "+%Y-%m-%d_%H%M"`
DOCKER_TAG=repairnator/pipeline:$DOCKER_VERSION

REPAIRNATOR_RUN_DIR=$HOME_REPAIR/bin/`date "+%Y-%m-%d_%H%M"`
REPAIRNATOR_DOCKER_DIR=$REPAIRNATOR_RUN_DIR/dockerImage

REPAIRNATOR_SCANNER_JAR="./repairnator-scanner/target/repairnator-scanner-*-jar-with-dependencies.jar"
REPAIRNATOR_SCANNER_DEST_JAR=$REPAIRNATOR_RUN_DIR/repairnator-scanner.jar

REPAIRNATOR_PIPELINE_JAR="./repairnator-pipeline/target/repairnator-pipeline-*-jar-with-dependencies.jar"
REPAIRNATOR_PIPELINE_DEST_JAR=$REPAIRNATOR_DOCKER_DIR/repairnator-pipeline.jar

REPAIRNATOR_BUILD_LIST=$REPAIRNATOR_RUN_DIR/list_build.txt

echo "Start building a new version of repairnator"
./build_repairnator.sh

if [[ $? != 0 ]]
then
   echo "Error while building a new version of repairnator"
   exit -1
fi

echo "Copy jar and prepare docker image"
mkdir $REPAIRNATOR_RUN_DIR
cp $REPAIRNATOR_SCANNER_JAR $REPAIRNATOR_SCANNER_DEST_JAR

mkdir $REPAIRNATOR_DOCKER_DIR
cp $REPAIRNATOR_PIPELINE_JAR $REPAIRNATOR_PIPELINE_DEST_JAR
cp -r $REPAIR_DOCKER_IMG_DIR/* $REPAIRNATOR_DOCKER_DIR

echo "Start to scan projects for builds..."
java -jar $REPAIRNATOR_SCANNER_DEST_JAR -m repair -g $GOOGLE_SECRET_PATH -i $REPAIR_PROJECT_LIST_PATH -o $REPAIRNATOR_BUILD_LIST

NB_LINES=`wc -l $REPAIRNATOR_BUILD_LIST`

if [[ $NB_LINES == 0 ]]
then
   echo "No build has been found. Stop the script now."
   exit 0
fi

echo "Build the docker machine..."
docker build -t $DOCKER_TAG --label version=$DOCKER_VERSION $REPAIRNATOR_DOCKER_DIR

i=0

REPAIR_ARGS="-m repair -f slug -i $PROJECT_LIST_PATH -o $OUTPUT_PATH -w $WORKSPACE_PATH -l $NB_HOURS -p --clean -z $Z3_PATH"

cd $WORKSPACE_PATH
java -Xmx1g -Xms1g -cp $TOOLS_PATH:$REPAIR_JAR fr.inria.spirals.repairnator.Launcher $REPAIR_ARGS &> $LOG_FILE

rm $REPAIR_JAR
#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")"; pwd)"

. $SCRIPT_DIR/utils/init_script.sh

CONFIG_PATH=$SCRIPT_DIR/config/repairnator-config.json
echo "Path of the config file: $CONFIG_PATH"

echo "Copy jar (repairnator-checkbranches:$CHECKBRANCHES_VERSION)..."
mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=fr.inria.repairnator:repairnator-checkbranches:$CHECKBRANCHES_VERSION:jar:jar-with-dependencies -Ddest=$REPAIRNATOR_CHECKBRANCHES_DEST_JAR

DOCKER_TAG=$(cat $CONFIG_PATH | sed 's/.*"dockerImageName": "\(.*\)".*/\1/;t;d')
echo "Pull the docker machine (name: $DOCKER_TAG)..."
docker pull $DOCKER_TAG

echo "Launch docker pool checkbranches..."
java $JAVA_OPTS -jar $REPAIRNATOR_CHECKBRANCHES_DEST_JAR --configPath $CONFIG_PATH &> $LOG_DIR/checkbranches.log

echo "Docker pool checkbranches finished, delete the run directory ($REPAIRNATOR_RUN_DIR)"
rm -rf $REPAIRNATOR_RUN_DIR

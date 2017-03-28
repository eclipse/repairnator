#!/bin/bash

export M2_HOME=
export GITHUB_LOGIN=
export GITHUB_OAUTH=
export HOME_REPAIR=
export NB_THREADS=4
export LOG_DIR=$HOME_REPAIR/logs/`date "+%Y-%m-%d_%H%M"`
export REPAIR_PROJECT_LIST_PATH=$HOME_REPAIR/scripts/project_list.txt
export REPAIR_OUTPUT_PATH=/var/www/html/repairnator/`date "+%Y-%m-%d_%H%M"`
export SCANNER_NB_HOURS=4
export SCANNER_MODE=repair
export DAY_TIMEOUT=1
export MONGODB_HOST=
export MONGODB_DBNAME=
export SPREADSHEET=
export PUSH_URL=

export REPAIRNATOR_GH_REPO_PATH=$HOME_REPAIR/github/librepair/repairnator
export REPAIR_DOCKER_IMG_DIR=$HOME_REPAIR/dockerImage/
export GOOGLE_SECRET_PATH=$HOME_REPAIR/client_secret.json
export REPAIRNATOR_SCANNED_DIR=$HOME_REPAIR/scanned/

export DOCKER_VERSION=`date "+%Y-%m-%d_%H%M"`
export DOCKER_TAG=repairnator/pipeline:$DOCKER_VERSION

export REPAIRNATOR_RUN_DIR=$HOME_REPAIR/bin/`date "+%Y-%m-%d_%H%M"`
export REPAIRNATOR_DOCKER_DIR=$REPAIRNATOR_RUN_DIR/dockerImage

export REPAIRNATOR_SCANNER_JAR="$REPAIRNATOR_GH_REPO_PATH/repairnator-scanner/target/repairnator-scanner-*-jar-with-dependencies.jar"
export REPAIRNATOR_SCANNER_DEST_JAR=$REPAIRNATOR_RUN_DIR/repairnator-scanner.jar

export REPAIRNATOR_DOCKERPOOL_JAR="$REPAIRNATOR_GH_REPO_PATH/repairnator-dockerpool/target/repairnator-dockerpool-*-jar-with-dependencies.jar"
export REPAIRNATOR_DOCKERPOOL_DEST_JAR=$REPAIRNATOR_RUN_DIR/repairnator-dockerpool.jar

export REPAIRNATOR_PIPELINE_JAR="$REPAIRNATOR_GH_REPO_PATH/repairnator-pipeline/target/repairnator-pipeline-*-jar-with-dependencies.jar"
export REPAIRNATOR_PIPELINE_DEST_JAR=$REPAIRNATOR_DOCKER_DIR/repairnator-pipeline.jar

export REPAIRNATOR_BUILD_LIST=$REPAIR_OUTPUT_PATH/list_build_`date "+%Y-%m-%d_%H%M"`.txt
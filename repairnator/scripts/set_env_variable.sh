#!/bin/bash

export M2_HOME=/opt/apache-maven-3.3.9
export PATH=$PATH:$M2_HOME/bin

export GITHUB_LOGIN=
export GITHUB_OAUTH=
export HOME_REPAIR=
export NB_THREADS=4
export LOG_DIR=$HOME_REPAIR/logs/`date "+%Y-%m-%d_%H%M"`

mkdir $LOG_DIR

export REPAIR_PROJECT_LIST_PATH=$HOME_REPAIR/scripts/project_list.txt
export REPAIR_OUTPUT_PATH=/var/www/html/repairnator/
export REPAIR_DOCKER_IMG_DIR=./dockerImage/
export GOOGLE_SECRET_PATH=./client_secret.json
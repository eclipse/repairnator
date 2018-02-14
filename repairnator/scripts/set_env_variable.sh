#!/bin/bash

#### STANDARD CONFIGURATION ####

export RUN_ID_SUFFIX= # A suffix to add to the run id.
export M2_HOME= # Path to maven home directory
export GITHUB_LOGIN= # Github Login to use github API
export GITHUB_OAUTH= # Github personal token (https://github.com/settings/tokens)
export HOME_REPAIR= # The home directory for the following paths
export NB_THREADS=4 # Number of concurrent docker container to run
export ROOT_LOG_DIR=$HOME_REPAIR/logs/
export LOG_DIR=$ROOT_LOG_DIR/`date "+%Y-%m-%d_%H%M"` # Log directory: it will contain several logs and serialized files
export DOCKER_LOG_DIR=$LOG_DIR # Log directory for docker containers (most of the time, it should be the same as LOG_DIR, but sometimes if you use distant host, e.g. g5k you need to specify another value)
export REPAIR_PROJECT_LIST_PATH=$HOME_REPAIR/scripts/project_list.txt # The list of project slug to scan
export REPAIR_OUTPUT_PATH=/var/www/html/repairnator/`date "+%Y-%m-%d_%H%M"` # Where to put output of the scanner
export SCANNER_NB_HOURS=4 # Limit of hours to inspect to get last builds
export SCANNER_MODE=repair # Available modes are repair or bears
export DAY_TIMEOUT=1 # Global timeout to stop the docker execution
export MONGODB_HOST= # Mongo host to put data, should follow the following format: mongodb://user:password@domain:port
export MONGODB_NAME= # Mongo database name to use
export SPREADSHEET= # Spreadsheet ID to put data
export PUSH_URL= # Github repository URL to put data (data are pushed in branches), on the format: https://github.com/user/repo
export SMTP_SERVER= # Smtp server to notify by email
export NOTIFY_TO= # email adresses separated by comma
export DOCKER_TAG=surli/repairnator:latest # Tag of the docker image to use for pipeline
export LOOK_FROM_DATE= # Use with the following one, when wanting to scan a period of time
export LOOK_TO_DATE=
export SKIP_LAUNCH_REPAIRNATOR=0 # Skip the launch of docker pool: it will only launch the scanner. Note that if a list of build is passed as parameter, this option is overriden.
export NOTIFY_ENDPROCESS=0 # If set to 1, the end of dockerpool and scan will send a notification using smtp and notify_to information
export CREATE_OUTPUT_DIR=0 # Use specifically for grid5000: allow to create a subdirectory to contain logs/serialization of docker containers

export CHECK_BRANCH_REPOSITORY= # Repository to use for check branches script
export HUMAN_PATCH=0 # Test the human patch for check branches ?

##### CONFIGURATION FOR REALTIME SCANNER ####

export JOB_SLEEP_TIME=10
export BUILD_SLEEP_TIME=10
export LIMIT_INSPECTED_BUILDS=100
export WHITELIST_PATH=
export BLACKLIST_PATH=

export DOCKER_CHECKBRANCHES_TAG=surli/checkbranches:latest
export REPAIRNATOR_GH_REPO_PATH=$HOME_REPAIR/github/librepair/repairnator # Path of the local cloned repository for repairnator
export GOOGLE_SECRET_PATH=$HOME_REPAIR/client_secret.json # Path of the google secret if spreadsheet is used

export REPAIRNATOR_RUN_DIR=$HOME_REPAIR/bin/`date "+%Y-%m-%d_%H%M"` # Where to put executables used (will be created automatically and deleted)

export REPAIRNATOR_SCANNER_JAR="$REPAIRNATOR_GH_REPO_PATH/repairnator-scanner/target/repairnator-scanner-*-jar-with-dependencies.jar" # full name of the scanner jar
export REPAIRNATOR_SCANNER_DEST_JAR=$REPAIRNATOR_RUN_DIR/repairnator-scanner.jar

export REPAIRNATOR_DOCKERPOOL_JAR="$REPAIRNATOR_GH_REPO_PATH/repairnator-dockerpool/target/repairnator-dockerpool-*-jar-with-dependencies.jar" # full name of docker pool jar
export REPAIRNATOR_DOCKERPOOL_DEST_JAR=$REPAIRNATOR_RUN_DIR/repairnator-dockerpool.jar

export REPAIRNATOR_BUILD_LIST=$REPAIR_OUTPUT_PATH/list_build_`date "+%Y-%m-%d_%H%M"`.txt # full name of the scanned build list

export REPAIRNATOR_CHECKBRANCHES_JAR="$REPAIRNATOR_GH_REPO_PATH/repairnator-checkbranches/target/repairnator-checkbranches-*-jar-with-dependencies.jar" # full name of the scanner jar
export REPAIRNATOR_CHECKBRANCHES_DEST_JAR=$REPAIRNATOR_RUN_DIR/repairnator-checkbranches.jar

export REPAIRNATOR_REALTIME_JAR="$REPAIRNATOR_GH_REPO_PATH/repairnator-realtime/target/repairnator-realtime-*-jar-with-dependencies.jar" # full name of the scanner jar
export REPAIRNATOR_REALTIME_DEST_JAR=$REPAIRNATOR_RUN_DIR/repairnator-realtime.jar
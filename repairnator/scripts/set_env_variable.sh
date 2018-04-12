#!/bin/bash

#### STANDARD CONFIGURATION ####

# Mandatory elements for all components of Repairnator

export GITHUB_OAUTH=XXXX # Github personal token (https://github.com/settings/tokens)
export HOME_REPAIR=$HOME/repairnator # The home directory for the following pathes

# Optional configuration for all components of Repairnator

export MONGODB_HOST= # Mongo host to put data, should follow the following format: mongodb://user:password@domain:port
export MONGODB_NAME= # Mongo database name to use
export PUSH_URL= # Github repository URL to put data (data are pushed in branches), on the format: https://github.com/user/repo
export SMTP_SERVER= # Smtp server to notify by email
export NOTIFY_TO= # email adresses separated by comma (used for notifier, you must have specify an smtp server)
export NOTIFY_ENDPROCESS=0 # If set to 1, the end of dockerpool and scanner will send a notification using smtp and notify_to information
export RUN_ID_SUFFIX= # A suffix to add to the run id (useful to retrieve a specific run in serialized data).

#### Scanner configuration

export REPAIR_PROJECT_LIST_PATH=$HOME_REPAIR/project_list.txt # The list of project slug to scan
export SCANNER_NB_HOURS=1 # Number of hours to inspect to get last builds (e.g. 4 will mean 4 hours in the past)
#export LOOK_FROM_DATE= # Use with the following one, when wanting to scan a period of time (format: dd/MM/yyyy)
#export LOOK_TO_DATE=

#### Docker pool configuration

export NB_THREADS=4 # Number of concurrent docker container to run
export DAY_TIMEOUT=1 # Global timeout to stop the docker execution

#### Realtime scanner configuration

export WHITELIST_PATH=$HOME_REPAIR/whitelist.txt # Path of the whitelist of projects (mandatory but the file does not have to exist)
export BLACKLIST_PATH=$HOME_REPAIR/blacklist.txt # Path of the blacklist (mandatory but the file does not have to exist)
export DURATION=PT10m # Duration execution of the process on the ISO-8601 duration format: PWdTXhYmZs (e.g. PT1h for 1 hour)
export JOB_SLEEP_TIME=10 # Sleep time in seconds for requesting /job endpoint in Travis
export BUILD_SLEEP_TIME=10 # Sleep time in seconds for refreshing builds status
export LIMIT_INSPECTED_BUILDS=100 # Maximum number of builds under inspection

#### Checkbranch configuration

export CHECK_BRANCH_REPOSITORY= # Repository to use for check branches script
export HUMAN_PATCH=0 # Test the human patch for check branches ?

##### ADVANCED CONFIGURATION

# Change the following configuration only if you know exactly what you're doing.

### Versions

export SCANNER_VERSION=LATEST
export DOCKERPOOL_VERSION=LATEST
export REALTIME_VERSION=LATEST
export PIPELINE_VERSION=LATEST

### Docker tags
export DOCKER_TAG=surli/repairnator:latest # Tag of the docker image to use for pipeline
export DOCKER_CHECKBRANCHES_TAG=surli/checkbranches:latest # Tag of the docker image to use for checkbranches

### Root pathes

export ROOT_LOG_DIR=$HOME_REPAIR/logs/ # The directory will be created if it does not exist
export ROOT_OUT_DIR=$HOME_REPAIR/out/ # The directory will be created if it does not exist
export ROOT_BIN_DIR=$HOME_REPAIR/bin/ # The directory will be created if it does not exist

### Binary pathes
export REPAIRNATOR_RUN_DIR=$ROOT_BIN_DIR`date "+%Y-%m-%d_%H%M"` # Where to put executables used (will be created automatically and deleted)
export REPAIRNATOR_SCANNER_DEST_JAR=$REPAIRNATOR_RUN_DIR/repairnator-scanner.jar
export REPAIRNATOR_DOCKERPOOL_DEST_JAR=$REPAIRNATOR_RUN_DIR/repairnator-dockerpool.jar
export REPAIRNATOR_CHECKBRANCHES_DEST_JAR=$REPAIRNATOR_RUN_DIR/repairnator-checkbranches.jar
export REPAIRNATOR_REALTIME_DEST_JAR=$REPAIRNATOR_RUN_DIR/repairnator-realtime.jar

### Other pathes
export REPAIR_OUTPUT_PATH=$ROOT_OUT_DIR`date "+%Y-%m-%d_%H%M%S"` # Where to put output of repairnator
export LOG_DIR=$ROOT_LOG_DIR`date "+%Y-%m-%d_%H%M"` # Log directory: it will contain several logs and serialized files
export DOCKER_LOG_DIR=$LOG_DIR # Log directory for docker containers (most of the time, it should be the same as LOG_DIR, but sometimes if you use distant host, e.g. g5k you need to specify another value)

export M2_HOME=$MAVEN_HOME # Path to the maven home: this value is only used when executing directly repairnator-pipeline.jar (outside a docker container)

### Switches

export BEARS_MODE=0 # Set 1 to use the bears mode
export BEARS_FIXER_MODE=both # Possible values are both, failing_passing or passing_passing
export SKIP_LAUNCH_REPAIRNATOR=0 # If set to 1, skip the launch of docker pool: it will only launch the scanner. Note that this option might be overriden in some scripts.
export CREATE_OUTPUT_DIR=0 # Use specifically for grid5000: allow to create a subdirectory to contain logs/serialization of docker containers


#!/bin/bash
set -e

# Use to create args in the command line for optional arguments
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
. $SCRIPT_DIR/utils/init_script.sh

echo "This will be run with the following RUN_ID: $RUN_ID"

source $SCRIPT_DIR/utils/create_structure.sh

if [ "$SKIP_SCAN" -eq 1 ]; then
    REPAIRNATOR_BUILD_LIST=$1
    SKIP_LAUNCH_REPAIRNATOR=0
else if [ ! -f "$REPAIR_PROJECT_LIST_PATH" ]; then
    touch $REPAIR_PROJECT_LIST_PATH
    fi
fi

echo "Copy jars"
mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=fr.inria.repairnator:repairnator-scanner:$SCANNER_VERSION:jar:jar-with-dependencies -Ddest=$REPAIRNATOR_SCANNER_DEST_JAR
mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=fr.inria.repairnator:repairnator-dockerpool:$DOCKERPOOL_VERSION:jar:jar-with-dependencies -Ddest=$REPAIRNATOR_DOCKERPOOL_DEST_JAR

if [ "$SKIP_SCAN" -eq 0 ]; then
    REPAIRNATOR_BUILD_LIST=$REPAIR_OUTPUT_PATH/list_build_`date "+%Y-%m-%d_%H%M"`_$RUN_ID.txt
    echo "Start to scan projects for builds (dest file: $REPAIRNATOR_BUILD_LIST)..."

    elementaryArgs="-i $REPAIR_PROJECT_LIST_PATH -o $REPAIRNATOR_BUILD_LIST --runId $RUN_ID"

    supplementaryArgs="`ca --dbhost $MONGODB_HOST`"
    supplementaryArgs="$supplementaryArgs `ca --dbname $MONGODB_NAME`"
    supplementaryArgs="$supplementaryArgs `ca -l $SCANNER_NB_HOURS`"
    supplementaryArgs="$supplementaryArgs `ca --lookFromDate $LOOK_FROM_DATE`"
    supplementaryArgs="$supplementaryArgs `ca --lookToDate $LOOK_TO_DATE`"
    supplementaryArgs="$supplementaryArgs `ca --smtpServer $SMTP_SERVER`"
    supplementaryArgs="$supplementaryArgs `ca --notifyto $NOTIFY_TO`"

    if [ "$BEARS_MDOE" -eq 1 ]; then
        supplementaryArgs="$supplementaryArgs --bears"
        supplementaryArgs="$supplementaryArgs --bearsMode $BEARS_FIXER_MODE"

        if [ "$BEARS_DELIMITER" -eq 1 ]; then
            supplementaryArgs="$supplementaryArgs --bearsDelimiter"
        fi
    fi

    if [ "$NOTIFY_ENDPROCESS" -eq 1 ]; then
        supplementaryArgs="$supplementaryArgs --notifyEndProcess"
    fi

    echo "Elementary args for scanner: $elementaryArgs"
    echo "Supplementary args for scanner: $supplementaryArgs"
    java -jar $REPAIRNATOR_SCANNER_DEST_JAR -d $elementaryArgs $supplementaryArgs &> $LOG_DIR/scanner_$RUN_ID.log
fi

NB_LINES=`wc -l < $REPAIRNATOR_BUILD_LIST`

if [[ $NB_LINES == 0 ]]; then
    echo "No build has been found. Stop the script now."
    exit 0
else
    echo "$NB_LINES builds have been found."
fi

if [ "$SKIP_LAUNCH_REPAIRNATOR" -eq 1 ]; then
    echo "Only scanning, skip the next steps"
    exit 0
fi

echo "Pull the docker machine (name: $DOCKER_TAG)..."
docker pull $DOCKER_TAG

echo "Launch docker pool..."

elementaryArgs="-t $NB_THREADS -n $DOCKER_TAG -i $REPAIRNATOR_BUILD_LIST -o $LOG_DIR -l $DOCKER_LOG_DIR -g $DAY_TIMEOUT --runId $RUN_ID --repairTools $REPAIR_TOOLS"

supplementaryArgs="`ca --dbhost $MONGODB_HOST``ca --dbname $MONGODB_NAME``ca --pushurl $PUSH_URL``ca --smtpServer $SMTP_SERVER``ca --notifyto $NOTIFY_TO`"

if [ "$BEARS_MODE" -eq 1 ]; then
    supplementaryArgs="$supplementaryArgs --bears"
fi

if [ "$NOTIFY_ENDPROCESS" -eq 1 ]; then
    supplementaryArgs="$supplementaryArgs --notifyEndProcess"
fi

if [ "$CREATE_OUTPUT_DIR" -eq 1 ]; then
    supplementaryArgs="$supplementaryArgs --createOutputDir"
fi

echo "Elementary args for docker pool: $elementaryArgs"
echo "Supplementary args for docker pool: $supplementaryArgs"
java -jar $REPAIRNATOR_DOCKERPOOL_DEST_JAR $elementaryArgs $supplementaryArgs &> $LOG_DIR/dockerpool_$RUN_ID.log

echo "Docker pool finished, delete the run directory ($REPAIRNATOR_RUN_DIR)"
rm -rf $REPAIRNATOR_RUN_DIR
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

if [ "$#" -eq 0 ]; then
    echo "A list of build ids must be provided"
    exit -1
fi

if [ ! -f $1 ]; then
    echo "The list of build ids must be an existing file ($1 not found)"
    exit -1
fi

REPAIRNATOR_BUILD_LIST=$1
NB_LINES=`wc -l < $REPAIRNATOR_BUILD_LIST`

if [[ $NB_LINES == 0 ]]; then
    echo "No build has been found. Stop the script now."
    exit 0
else
    echo "$NB_LINES builds have been found."
fi

if [ "$#" -eq 1 ]; then
    SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

    . $SCRIPT_DIR/utils/init_script.sh
fi

echo "Copy jar into $REPAIRNATOR_DOCKERPOOL_DEST_JAR"
mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=fr.inria.repairnator:repairnator-dockerpool:$DOCKERPOOL_VERSION:jar:jar-with-dependencies -Ddest=$REPAIRNATOR_DOCKERPOOL_DEST_JAR

echo "Pull the docker machine (name: $DOCKER_TAG)..."
docker pull $DOCKER_TAG

echo "Launch docker pool..."

elementaryArgs="-t $NB_THREADS -n $DOCKER_TAG -i $REPAIRNATOR_BUILD_LIST -o $LOG_DIR -l $DOCKER_LOG_DIR -g $DAY_TIMEOUT --runId $RUN_ID --ghOauth $GITHUB_OAUTH --repairTools $REPAIR_TOOLS"

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

echo "Docker pool finished."
if [ "$#" -eq 1 ]; then
    echo "Delete the run directory ($REPAIRNATOR_RUN_DIR)."
    rm -rf $REPAIRNATOR_RUN_DIR
fi
#!/bin/bash
set -e

function usage {
    echo "This script aims at launching repairnator on a given list of TravisCI build ids."
    echo "Error: $1"
    echo "Usage: launch_dockerpool.sh <path_to_file>"
    exit -1
}

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

if [ "$#" -ne 1 ]; then
    usage "A file with a list of build ids must be provided"
fi

if [ ! -f $1 ]; then
    usage "The file with the list of build ids must exist ($1 not found)"
fi

REPAIRNATOR_BUILD_LIST=$1
NB_LINES=`wc -l < $REPAIRNATOR_BUILD_LIST`

if [[ $NB_LINES == 0 ]]; then
    echo "No build has been found. Stop the script now."
    exit 0
else
    echo "$NB_LINES builds have been found."
fi

DELETE_RUN_DIR=0
if [ -z "$REPAIRNATOR_INITIALIZED" ]; then
    SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

    . $SCRIPT_DIR/utils/init_script.sh

    DELETE_RUN_DIR=1
fi

echo "Copy jar into $REPAIRNATOR_DOCKERPOOL_DEST_JAR"
mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=fr.inria.repairnator:repairnator-dockerpool:$DOCKERPOOL_VERSION:jar:jar-with-dependencies -DremoteRepositories=ossSnapshot::::https://oss.sonatype.org/content/repositories/snapshots -Ddest=$REPAIRNATOR_DOCKERPOOL_DEST_JAR

if [ "$BEARS_MODE" -eq 1 ]; then
    DOCKER_TAG=$DOCKER_TAG_BEARS
fi

echo "Pull the docker machine (name: $DOCKER_TAG)..."
docker pull $DOCKER_TAG

echo "Launch docker pool..."

elementaryArgs="-t $NB_THREADS -n $DOCKER_TAG -i $REPAIRNATOR_BUILD_LIST -o $LOG_DIR -l $DOCKER_LOG_DIR -g $DAY_TIMEOUT --runId $RUN_ID --ghOauth $GITHUB_OAUTH --repairTools $REPAIR_TOOLS"

if [ "$SKIP_DELETE" -eq 1 ]; then
    elementaryArgs="$elementaryArgs --skipDelete"
fi

supplementaryArgs="`ca --dbhost $MONGODB_HOST``ca --dbname $MONGODB_NAME``ca --pushurl $PUSH_URL``ca --smtpServer $SMTP_SERVER``ca --notifyto $NOTIFY_TO`"
supplementaryArgs="$supplementaryArgs`ca --githubUserName $GITHUB_USERNAME``ca --githubUserEmail $GITHUB_USEREMAIL`"

if [ "$BEARS_MODE" -eq 1 ]; then
    supplementaryArgs="$supplementaryArgs --bears"
fi

if [ "$NOTIFY_ENDPROCESS" -eq 1 ]; then
    supplementaryArgs="$supplementaryArgs --notifyEndProcess"
fi

if [ "$CREATE_OUTPUT_DIR" -eq 1 ]; then
    supplementaryArgs="$supplementaryArgs --createOutputDir"
fi

if [ "$CREATE_PR" -eq 1 ]; then
    args="$args --createPR"
fi

echo "Elementary args for docker pool: $elementaryArgs"
echo "Supplementary args for docker pool: $supplementaryArgs"
java $JAVA_OPTS -jar $REPAIRNATOR_DOCKERPOOL_DEST_JAR -d $elementaryArgs $supplementaryArgs &> $LOG_DIR/dockerpool_$RUN_ID.log

echo "Docker pool finished."
if [ "$DELETE_RUN_DIR" -eq 1 ]; then
    echo "Delete the run directory ($REPAIRNATOR_RUN_DIR)."
    rm -rf $REPAIRNATOR_RUN_DIR
fi

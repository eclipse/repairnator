#!/usr/bin/env bash
set -e

if [ "$#" -ne 2 ]; then
    echo "Usage: ./check_branches.sh <input branch names> <output result>"
    exit 2
fi

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


if [ ! -f $1 ]; then
    echo "The list of input branches must be an existing file ($1 not found)"
    exit -1
fi

if [ -f $2 ]; then
    echo "The output file already exists, please specify a path for a new file to create ($2 already exists)"
    exit -1
fi

INPUT=$1
OUTPUT=$2

touch $OUTPUT

SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

. $SCRIPT_DIR/utils/init_script.sh

echo "Copy jar and prepare docker image"
mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=fr.inria.repairnator:repairnator-checkbranches:$CHECKBRANCHES_VERSION:jar:jar-with-dependencies -Ddest=$REPAIRNATOR_CHECKBRANCHES_DEST_JAR

if [ "$BEARS_MODE" -eq 1 ]; then
    DOCKER_CHECKBRANCHES_TAG=$DOCKER_CHECKBRANCHES_TAG_BEARS
fi

echo "Pull the docker machine (name: $DOCKER_CHECKBRANCHES_TAG)..."
docker pull $DOCKER_CHECKBRANCHES_TAG

echo "Analyze started: `date "+%Y-%m-%d_%H%M%S"`" > $OUTPUT
echo "Considered repository: $CHECK_BRANCH_REPOSITORY" >> $OUTPUT

echo "Launch docker pool checkbranches ..."
args="`ca --smtpServer $SMTP_SERVER``ca --notifyto $NOTIFY_TO`"
if [ "$NOTIFY_ENDPROCESS" -eq 1 ]; then
    args="$args --notifyEndProcess"
fi
if [ "$HUMAN_PATCH" -eq 1 ]; then
    args="$args --humanPatch"
fi
if [ "$SKIP_DELETE" -eq 1 ]; then
    args="$args --skipDelete"
fi
if [ "$BEARS_MODE" -eq 1 ]; then
    args="$args --bears"
fi

echo "Supplementary args for docker pool checkbranches $args"
java $JAVA_OPTS -jar $REPAIRNATOR_CHECKBRANCHES_DEST_JAR -t $NB_THREADS -n $DOCKER_CHECKBRANCHES_TAG -i $INPUT -o $OUTPUT -r $CHECK_BRANCH_REPOSITORY -g $DAY_TIMEOUT --runId $RUN_ID $args &> $LOG_DIR/checkbranches.log

echo "Docker pool checkbranches finished, delete the run directory ($REPAIRNATOR_RUN_DIR)"
rm -rf $REPAIRNATOR_RUN_DIR

echo "Analyze finished: `date "+%Y-%m-%d_%H%M%S"`" >> $OUTPUT

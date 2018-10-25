#!/bin/bash
set -e

# Use to create args in the command line for optionnal arguments
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

SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

. $SCRIPT_DIR/utils/init_script.sh

if [ ! -f "$WHITELIST_PATH" ]; then
    touch $WHITELIST_PATH
fi
if [ ! -f "$BLACKLIST_PATH" ]; then
    touch $BLACKLIST_PATH
fi

echo "Copy jars and prepare docker image"
mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=fr.inria.repairnator:repairnator-realtime:$REALTIME_VERSION:jar:jar-with-dependencies -DremoteRepositories=ossSnapshot::::https://oss.sonatype.org/content/repositories/snapshots -Ddest=$REPAIRNATOR_REALTIME_DEST_JAR

echo "Pull the docker machine (name: $DOCKER_TAG)..."
docker pull $DOCKER_TAG

echo "Launch repairnator realtime scanner..."
args="`ca --dbhost $MONGODB_HOST``ca --dbname $MONGODB_NAME``ca --pushurl $PUSH_URL`"
args="$args`ca --smtpServer $SMTP_SERVER``ca --notifyto $NOTIFY_TO``ca --jobsleeptime $JOB_SLEEP_TIME`"
args="$args`ca --buildsleeptime $BUILD_SLEEP_TIME``ca --maxinspectedbuilds $LIMIT_INSPECTED_BUILDS``ca --whitelist $WHITELIST_PATH`"
args="$args`ca --blacklist $BLACKLIST_PATH``ca --duration $DURATION`"
args="$args`ca --githubUserName $GITHUB_USERNAME``ca --githubUserEmail $GITHUB_USEREMAIL`"

if [ "$NOTIFY_ENDPROCESS" -eq 1 ]; then
    args="$args --notifyEndProcess"
fi

if [ "$CREATE_OUTPUT_DIR" -eq 1 ]; then
    args="$args --createOutputDir"
fi

if [ "$SKIP_DELETE" -eq 1 ]; then
    args="$args --skipDelete"
fi

if [ "$CREATE_PR" -eq 1 ]; then
    args="$args --createPR"
fi

echo "Supplementary args for realtime scanner $args"
java $JAVA_OPTS -jar $REPAIRNATOR_REALTIME_DEST_JAR -t $NB_THREADS -n $DOCKER_TAG -o $LOG_DIR -l $DOCKER_LOG_DIR --runId $RUN_ID --ghOauth $GITHUB_OAUTH --repairTools $REPAIR_TOOLS $args 2> $LOG_DIR/errors_$RUN_ID.log 1> /dev/null

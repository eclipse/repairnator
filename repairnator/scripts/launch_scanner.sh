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

DELETE_RUN_DIR=0
if [ -z "$REPAIRNATOR_INITIALIZED" ]; then
    SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

    . $SCRIPT_DIR/utils/init_script.sh

    DELETE_RUN_DIR=1
fi

echo "Copy jar into $REPAIRNATOR_SCANNER_DEST_JAR"
mvn org.apache.maven.plugins:maven-dependency-plugin:2.8:get -Dartifact=fr.inria.repairnator:repairnator-scanner:$SCANNER_VERSION:jar:jar-with-dependencies -DremoteRepositories=ossSnapshot::::https://oss.sonatype.org/content/repositories/snapshots -Ddest=$REPAIRNATOR_SCANNER_DEST_JAR

REPAIRNATOR_BUILD_LIST=$REPAIR_OUTPUT_PATH/list_build_`date "+%Y-%m-%d_%H%M"`_$RUN_ID.txt

echo "Start to scan projects for builds (dest file: $REPAIRNATOR_BUILD_LIST)..."

elementaryArgs="-i $REPAIR_PROJECT_LIST_PATH -o $REPAIRNATOR_BUILD_LIST --runId $RUN_ID --ghOauth $GITHUB_OAUTH"

supplementaryArgs="`ca --dbhost $MONGODB_HOST`"
supplementaryArgs="$supplementaryArgs `ca --dbname $MONGODB_NAME`"
supplementaryArgs="$supplementaryArgs `ca -l $SCANNER_NB_HOURS`"
supplementaryArgs="$supplementaryArgs `ca --lookFromDate $LOOK_FROM_DATE`"
supplementaryArgs="$supplementaryArgs `ca --lookToDate $LOOK_TO_DATE`"
supplementaryArgs="$supplementaryArgs `ca --smtpServer $SMTP_SERVER`"
supplementaryArgs="$supplementaryArgs `ca --notifyto $NOTIFY_TO`"

if [ "$BEARS_MODE" -eq 1 ]; then
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
java $JAVA_OPTS -jar $REPAIRNATOR_SCANNER_DEST_JAR -d $elementaryArgs $supplementaryArgs &> $LOG_DIR/scanner_$RUN_ID.log

echo "Scanner finished."
if [ "$DELETE_RUN_DIR" -eq 1 ]; then
    echo "Delete the run directory ($REPAIRNATOR_RUN_DIR)."
    rm -rf $REPAIRNATOR_RUN_DIR
fi

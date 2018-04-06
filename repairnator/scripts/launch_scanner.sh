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

SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

echo "Set environment variables"
source $SCRIPT_DIR/set_env_variable.sh

echo "Create output directory: $REPAIR_OUTPUT_PATH"
mkdir -p $REPAIR_OUTPUT_PATH

echo "Create log directory: $LOG_DIR"
mkdir -p $LOG_DIR

echo "Create bin directory: $REPAIRNATOR_RUN_DIR"
mkdir -p $REPAIRNATOR_RUN_DIR

if [ -z "$RUN_ID_SUFFIX" ]; then
    RUN_ID=`uuidgen`
else
    RUN_ID=`uuidgen`_$RUN_ID_SUFFIX
fi

echo "This will be run with the following RUN_ID: $RUN_ID"

echo "Start building a new version of repairnator"
$SCRIPT_DIR/build_repairnator.sh

if [[ $? != 0 ]]
then
   echo "Error while building a new version of repairnator"
   exit -1
fi

echo "Copy jars"
cp $REPAIRNATOR_SCANNER_JAR $REPAIRNATOR_SCANNER_DEST_JAR

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
fi

if [ "$NOTIFY_ENDPROCESS" -eq 1 ]; then
    supplementaryArgs="$supplementaryArgs --notifyEndProcess"
fi

echo "Elementary args for scanner: $elementaryArgs"
echo "Supplementary args for scanner: $supplementaryArgs"
java -jar $REPAIRNATOR_SCANNER_DEST_JAR -d $elementaryArgs $supplementaryArgs &> $LOG_DIR/scanner_$RUN_ID.log

echo "Scanner finished, delete the run directory ($REPAIRNATOR_RUN_DIR)"
rm -rf $REPAIRNATOR_RUN_DIR
#!/usr/bin/env bash
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

elementaryArgs="-i $REPAIR_PROJECT_LIST_PATH -o $REPAIRNATOR_BUILD_LIST --runId $RUN_ID --ghOauth $GITHUB_OAUTH"

supplementaryArgs="`ca --dbhost $MONGODB_HOST`"
supplementaryArgs="$supplementaryArgs `ca --dbname $MONGODB_NAME`"
supplementaryArgs="$supplementaryArgs `ca -l $SCANNER_NB_HOURS`"
supplementaryArgs="$supplementaryArgs `ca --lookFromDate $LOOK_FROM_DATE`"
supplementaryArgs="$supplementaryArgs `ca --lookToDate $LOOK_TO_DATE`"
supplementaryArgs="$supplementaryArgs `ca --smtpServer $SMTP_SERVER`"
supplementaryArgs="$supplementaryArgs `ca --smtpPort $SMTP_PORT`"
supplementaryArgs="$supplementaryArgs `ca --smtpUsername $SMTP_USERNAME`"
supplementaryArgs="$supplementaryArgs `ca --smtpPassword $SMTP_PASSWORD`"
supplementaryArgs="$supplementaryArgs `ca --notifyto $NOTIFY_TO`"



if [ "$SMTP_TLS" -eq 1 ]; then
    supplementaryArgs="$args --smtpTLS"
fi

if [ "$NOTIFY_ENDPROCESS" -eq 1 ]; then
    supplementaryArgs="$supplementaryArgs --notifyEndProcess"
fi

echo "Elementary args for scanner: $elementaryArgs"
echo "Supplementary args for scanner: $supplementaryArgs"

# Clean up env variables
export MONGODB_HOST=
export MONGODB_NAME=
export SMTP_SERVER=
export SMTP_PORT=
export SMTP_USERNAME=
export SMTP_PASSWORD=
export SMTP_TLS=
export NOTIFY_TO=
export NOTIFY_ENDPROCESS=
export REPAIR_PROJECT_LIST_PATH=
export REPAIRNATOR_BUILD_LIST=
export RUN_ID=
export JAVA_OPTS=
export LOG_DIR=
export SCANNER_NB_HOURS=


java $JAVA_OPTS -jar /root/scanner.jar -d $elementaryArgs $supplementaryArgs 


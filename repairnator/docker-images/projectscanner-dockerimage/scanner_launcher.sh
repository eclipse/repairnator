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
# will be --activemqsubmitqueuename later when the snapshot is updated.
elementaryArgs="-i $REPAIR_PROJECT_LIST_PATH -o $REPAIRNATOR_BUILD_LIST --runId $RUN_ID --pipelinemode $PIPELINE_MODE --activemqurl $ACTIVEMQ_URL --activemqlistenqueuename $ACTIVEMQ_LISTEN_QUEUE_NAME --activemqqueuename $ACTIVEMQ_SUBMIT_QUEUE_NAME --ghOauth $GITHUB_OAUTH"

supplementaryArgs="`ca --dbhost $MONGODB_HOST`"
supplementaryArgs="$supplementaryArgs `ca --dbname $MONGODB_NAME`"
supplementaryArgs="$supplementaryArgs `ca -l $SCANNER_NB_HOURS`"
supplementaryArgs="$supplementaryArgs `ca --lookFromDate $LOOK_FROM_DATE`"
supplementaryArgs="$supplementaryArgs `ca --lookToDate $LOOK_TO_DATE`"
supplementaryArgs="$supplementaryArgs `ca --smtpServer $SMTP_SERVER`"
supplementaryArgs="$supplementaryArgs `ca --smtpPort $SMTP_PORT`"
supplementaryArgs="$supplementaryArgs `ca --smtpUsername $SMTP_USERNAME`"
supplementaryArgs="$supplementaryArgs `ca --smtpPassword $SMTP_PASSWORD`"



if [ "$SMTP_TLS" -eq 1 ]; then
    supplementaryArgs="$args --smtpTLS"
fi

if [ "$NOTIFY_ENDPROCESS" -eq 1 ]; then
    supplementaryArgs="$supplementaryArgs --notifyEndProcess"
fi

echo "Elementary args for scanner: $elementaryArgs"
echo "Supplementary args for scanner: $supplementaryArgs"


java $JAVA_OPTS -jar /root/repairnator-projectscanner.jar -d $elementaryArgs $supplementaryArgs 

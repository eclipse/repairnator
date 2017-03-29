#!/usr/bin/env bash

# Use to create args in the command line for optionnal arguments
function ca {
  if [ -z "$2" ];
  then
      echo ""
  else
      echo "$1 $2 "
  fi
}

args="`ca -g $GOOGLE_ACCESS_TOKEN``ca --spreadsheet $SPREADSHEET_ID``ca --dbhome $MONGODB_HOME``ca --dbname $MONGODB_NAME``ca --pushurl $PUSH_URL`"

java -cp $JAVA_HOME/lib/tools.jar:repairnator-pipeline.jar -Dlogback.configurationFile=/root/logback.xml fr.inria.spirals.repairnator.pipeline.Launcher -m $REPAIR_MODE -d -b $BUILD_ID --runId $RUN_ID -o $OUTPUT $args
#!/usr/bin/env bash

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

args="`ca --dbhost $MONGODB_HOST``ca --dbname $MONGODB_NAME``ca --pushurl $PUSH_URL``ca --smtpServer $SMTP_SERVER``ca --smtpPort $SMTP_PORT``ca --smtpUsername $SMTP_USERNAME``ca --smtpPassword $SMTP_PASSWORD``ca --notifyto $NOTIFY_TO``ca --githubUserName $GITHUB_USERNAME``ca --githubUserEmail $GITHUB_USEREMAIL``ca --experimentalPluginRepoList $EXPERIMENTAL_PLUGIN_REPOS`"

if [ "$CREATE_PR" -eq 1 ]; then
  args="$args --createPR"
fi

if [ "$REPAIR_MODE" = "checkstyle" ]; then
  args="$args --checkstyle"
fi

if [ "$SMTP_TLS" -eq 1 ]; then
    args="$args --smtpTLS"
fi

if [ ! -d "$OUTPUT" ]; then
    mkdir $OUTPUT
fi

# Clean env variables
export MONGODB_HOST=
export MONGODB_NAME=
export PUSH_URL=
export SMTP_SERVER=
export SMTP_PORT=
export SMTP_USERNAME=
export SMTP_PASSWORD=
export SMTP_TLS=
export NOTIFY_TO=
export EXPERIMENTAL_PLUGIN_REPOS=

LOCAL_REPAIR_MODE=$REPAIR_MODE

LOCAL_BUILD_ID=$BUILD_ID
export BUILD_ID=

LOCAL_RUN_ID=$RUN_ID
export RUN_ID=

LOCAL_OUTPUT=$OUTPUT
export OUTPUT=

LOCAL_GITHUB_OAUTH=$GITHUB_OAUTH
export GITHUB_OAUTH=

LOCAL_GITHUB_USERNAME=$GITHUB_USERNAME
export GITHUB_USERNAME=

LOCAL_GITHUB_USERNAME=$GITHUB_USEREMAIL
export GITHUB_USEREMAIL=

echo "Execute pipeline with following supplementary args: $args"
java -cp $JAVA_HOME/lib/tools.jar:repairnator-pipeline.jar -Dlogback.configurationFile=/root/logback.xml fr.inria.spirals.repairnator.pipeline.Launcher -d -b $LOCAL_BUILD_ID --runId $LOCAL_RUN_ID -o $LOCAL_OUTPUT --ghOauth $LOCAL_GITHUB_OAUTH --repairTools $REPAIR_TOOLS $args

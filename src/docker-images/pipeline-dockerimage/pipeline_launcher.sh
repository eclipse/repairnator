#!/usr/bin/env bash

# Use to create args in the command line for optional arguments
function ca {
  if [[ -z "$2" || "$2" == "null" ]];
  then
      echo ""
  else
    echo "$1 $2 "
  fi
}

args="`ca --dbhost $MONGODB_HOST`\
`ca --dbname $MONGODB_NAME`\
`ca --smtpServer $SMTP_SERVER`\
`ca --smtpPort $SMTP_PORT`\
`ca --smtpUsername $SMTP_USERNAME`\
`ca --smtpPassword $SMTP_PASSWORD`\
`ca --notifyto $NOTIFY_TO`\
`ca --launcherChoice NEW`\
`ca --launcherMode GIT_REPOSITORY`\
`ca --githubUserName $GITHUB_USERNAME`\
`ca --githubUserEmail $GITHUB_USEREMAIL`\
`ca --experimentalPluginRepoList $EXPERIMENTAL_PLUGIN_REPOS`\
`ca --listenermode $LISTEN_MODE`\
`ca --activemqurl $ACTIVEMQ_URL`\
`ca --activemqlistenqueuename $ACTIVEMQ_LISTEN_QUEUE`\
`ca --activemqusername $ACTIVEMQ_USERNAME`\
`ca --activemqpassword $ACTIVEMQ_PASSWORD`\
`ca --pushurl $PUSH_URL`\
`ca --jtravisendpoint $TRAVIS_ENDPOINT`\
`ca --travistoken $TRAVIS_TOKEN`\
`ca --sonarRules $SORALD_SONAR_RULES`\
`ca --soraldRepairMode $SORALD_REPAIR_MODE`\
`ca --soraldSegmentSize $SORALD_SEGMENT_SIZE`\
`ca --soraldMaxFixesPerRule $SORALD_MAX_FIXES_PER_RULE`\
`ca --soraldSkipPR $SORALD_SKIP_PR`"

if [[ "$CREATE_PR" == 1 ]]; then
  args="$args --createPR"
fi

if [[ "$SMTP_TLS" == 1 ]]; then
    args="$args --smtpTLS"
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
export TRAVIS_ENDPOINT=
export TRAVIS_TOKEN=

LOCAL_REPAIR_MODE=repair


# Github XOR Travis
if [[ -n "$GITHUB_URL" ]] && [[ -n "$GITHUB_SHA" ]]; then
  echo "adding GitHub mode variables"
  args="$args --gitrepo --gitrepourl $GITHUB_URL --gitrepoidcommit $GITHUB_SHA"
elif [[ -n "$BUILD_ID" ]]; then
  echo "adding Travis mode variables"
  args="$args -b $BUILD_ID"
else
  echo "Error: Neither GitHub mode nor Travis mode parameters hve been provided correctly."
  exit -1
fi

export GITHUB_URL=
export GITHUB_SHA=
export BUILD_ID=

if [[ -z "$RUN_ID" ]]; then
    RUN_ID=1234
fi

LOCAL_RUN_ID=$RUN_ID

#### REPAIR_TOOLS
if [[ -z "$REPAIR_TOOLS" ]]; then
    REPAIR_TOOLS=NPEFix
fi


#### OUTPUT DIR
if [[ -z "$OUTPUT" ]]; then
    OUTPUT=./
fi

if [[ ! -d "$OUTPUT" ]]; then
    mkdir $OUTPUT
fi

LOCAL_OUTPUT=$OUTPUT
export OUTPUT=

if [[ -z "$GITHUB_OAUTH" ]]; then
    # the java code falls back to no token if this one is invalid
    GITHUB_OAUTH=invalid_token
fi

LOCAL_GITHUB_OAUTH=$GITHUB_OAUTH
export GITHUB_OAUTH=

LOCAL_GITHUB_USERNAME=$GITHUB_USERNAME
export GITHUB_USERNAME=

LOCAL_GITHUB_USERNAME=$GITHUB_USEREMAIL
export GITHUB_USEREMAIL=

echo "Execute pipeline with following supplementary args: $args"

java -cp $JAVA_HOME/lib/tools.jar:repairnator-pipeline.jar -Dlogback.configurationFile=/root/logback.xml fr.inria.spirals.repairnator.pipeline.Launcher -d --runId $LOCAL_RUN_ID -o $LOCAL_OUTPUT --ghOauth $LOCAL_GITHUB_OAUTH --repairTools $REPAIR_TOOLS $args -d

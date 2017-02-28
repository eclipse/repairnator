#!/bin/bash

HOME_REPAIR=/media/experimentations/repairnator

RUNNING_REPAIR=`ps aux |grep "[r]epairnator.jar" |wc -l`
if [[ $RUNNING_REPAIR == 0 ]]
then
   $HOME_REPAIR/scripts/build_repairnator.sh
fi

export M2_HOME=/opt/apache-maven-3.3.9
export GITHUB_OAUTH=
TOOLS_PATH=/usr/lib/jvm/java-8-openjdk-amd64/lib/tools.jar
REPAIR_JAR=$HOME_REPAIR/scripts/repairnator.jar

LOG_FILE=$HOME_REPAIR/logs/output_`date "+%Y-%m-%d_%H%M"`.log
NB_HOURS=6
Z3_PATH=$HOME_REPAIR/github/nopol/nopol/lib/z3/z3_for_linux
WORKSPACE_PATH=$HOME_REPAIR/workspace/
OUTPUT_PATH=/var/www/html/repairnator/
PROJECT_LIST_PATH=$HOME_REPAIR/scripts/project_list.txt

REPAIR_ARGS="-m slug -i $PROJECT_LIST_PATH -o $OUTPUT_PATH -w $WORKSPACE_PATH -z $Z3_PATH -p -l $NB_HOURS --clean"

cd $WORKSPACE_PATH
java -Xmx1g -Xms1g -cp $TOOLS_PATH:$REPAIR_JAR fr.inria.spirals.repairnator.Launcher $REPAIR_ARGS &> $LOG_FILE

#!/bin/bash

RUNNING_REPAIR=`ps aux |grep "[r]epairnator.jar" |wc -l`
if [[ $RUNNING_REPAIR == 0 ]]
then
   /home/repairnator/scripts/build_repairnator.sh
fi

export M2_HOME=/opt/apache-maven-3.3.9
export GITHUB_OAUTH=
TOOLS_PATH=/usr/lib/jvm/java-8-oracle/lib/tools.jar
REPAIR_JAR=/home/repairnator/scripts/repairnator.jar

LOG_FILE=/home/repairnator/logs/output_`date "+%Y-%m-%d_%H%M"`.log
NB_HOURS=4
Z3_PATH=/home/repairnator/github/nopol/nopol/lib/z3/z3_for_linux
WORKSPACE_PATH=/home/repairnator/workspace/
OUTPUT_PATH=/var/www/html/repair/
PROJECT_LIST_PATH=/home/repairnator/scripts/project_list.txt

REPAIR_ARGS="-m slug -i $PROJECT_LIST_PATH -o $OUTPUT_PATH -w $WORKSPACE_PATH -z $Z3_PATH -p -l $NB_HOURS --clean"

cd $WORKSPACE_PATH
java -Xmx1g -Xms1g -cp $TOOLS_PATH:$REPAIR_JAR fr.inria.spirals.repairnator.Launcher $REPAIR_ARGS &> $LOG_FILE
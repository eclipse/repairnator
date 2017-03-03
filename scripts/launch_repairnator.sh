#!/bin/bash

export GITHUB_LOGIN=
export GITHUB_OAUTH=
HOME_REPAIR=

REPAIRNATOR_JARFILE="./repairnator-scanner/target/repairnator-scanner-1.0-SNAPSHOT-jar-with-dependencies.jar"
REPAIR_JAR=$HOME_REPAIR/bin/`date "+%Y-%m-%d_%H%M"`_repairnator.jar

export M2_HOME=/opt/apache-maven-3.3.9
export PATH=$PATH:$M2_HOME/bin

cd $HOME_REPAIR/github/nopol/nopol
git pull
mvn clean install

cd $HOME_REPAIR/github/librepair
git pull

cd jtravis
mvn clean install

cd ../repairnator
mvn clean install

if [[ $? == 0 ]]
then
   cp -f $REPAIRNATOR_JARFILE $REPAIR_JAR
else
   echo "Error while building a new version of repairnator"
fi

export M2_HOME=/opt/apache-maven-3.3.9

TOOLS_PATH=/usr/lib/jvm/java-8-openjdk-amd64/lib/tools.jar

LOG_FILE=$HOME_REPAIR/logs/output_`date "+%Y-%m-%d_%H%M"`.log
NB_HOURS=6
Z3_PATH=$HOME_REPAIR/github/nopol/nopol/lib/z3/z3_for_linux
WORKSPACE_PATH=$HOME_REPAIR/workspace/
OUTPUT_PATH=/var/www/html/repairnator/
PROJECT_LIST_PATH=$HOME_REPAIR/scripts/project_list.txt

REPAIR_ARGS="-m slug -i $PROJECT_LIST_PATH -o $OUTPUT_PATH -w $WORKSPACE_PATH -z $Z3_PATH -p -l $NB_HOURS --clean"

cd $WORKSPACE_PATH
java -Xmx1g -Xms1g -cp $TOOLS_PATH:$REPAIR_JAR fr.inria.spirals.repairnator.Launcher $REPAIR_ARGS &> $LOG_FILE

rm $REPAIR_JAR
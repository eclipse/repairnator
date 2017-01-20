#!/bin/bash

export GITHUB_LOGIN=
export GITHUB_OAUTH=
export JAVA_HOME="/usr/lib/jvm/java-8-openjdk-amd64/jre"
export M2_HOME="/usr/share/maven"
cd /opt/repairnator
# launch repairnator with following args:
# -d : debug mode
# -l 6 : get failing builds of the 6 last hours
# -i project_list.txt : consider project_list.txt as input for project names
# -o /var/... : output the json at the following location
# -p : push failing builds
# -w : specify the workspace path
# -z : specify the path for the solver used by Nopol
java -Xmx1024m -cp $JAVA_HOME/../lib/tools.jar:repairnator.jar fr.inria.spirals.repairnator.Launcher --clean -d -p -l 4 -i project_list.txt -w /media/experimentations/repairnator/bot -z /opt/repairnator/nopol/lib/z3/z3_for_linux -o /var/www/html/repairnator/ &> /var/log/repairnator/output_bot_`date "+%Y-%m-%d_%H%M"`.log
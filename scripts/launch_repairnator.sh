#!/bin/bash

#export GITHUB_LOGIN=login
#export GITHUB_OAUTH=token
#export M2_HOME=mvn home

cd /opt/repairnator
# launch repairnator with following args:
# -d : debug mode
# -l 6 : get failing builds of the 6 last hours
# -i project_list.txt : consider project_list.txt as input for project names
# -o /var/... : output the json at the following location
# -p : push failing builds
# -w : specify the workspace path
# -z : specify the path for the solver used by Nopol
java -jar repairnator.jar -d -p -l 4 -i project_list.txt -w /media/experimentations/repairnator -z /opt/repairnator/nopol/lib/z3/z3_for_linux -o /var/www/html/repairnator/ &> /var/log/repairnator/output_`date "+%Y-%m-%d_%H%M"`.log

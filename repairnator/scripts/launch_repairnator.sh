#!/bin/bash
set -e

# Use to create args in the command line for optional arguments
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

SKIP_SCAN=0
if [ "$#" -eq 1 ]; then
    if [ ! -f $1 ]; then
        echo "The list of build ids must be an existing file ($1 not found)"
        exit -1
    else
        SKIP_SCAN=1
    fi
fi

SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

. $SCRIPT_DIR/utils/init_script.sh

if [ "$SKIP_SCAN" -eq 1 ]; then
    REPAIRNATOR_BUILD_LIST=$1
    SKIP_LAUNCH_REPAIRNATOR=0
else if [ ! -f "$REPAIR_PROJECT_LIST_PATH" ]; then
    touch $REPAIR_PROJECT_LIST_PATH
    fi
fi

if [ "$SKIP_SCAN" -eq 0 ]; then
    . $SCRIPT_DIR/launch_scanner.sh
fi

if [ "$SKIP_LAUNCH_REPAIRNATOR" -eq 0 ]; then
    . $SCRIPT_DIR/launch_dockerpool.sh $REPAIRNATOR_BUILD_LIST
fi

echo "Delete the run directory ($REPAIRNATOR_RUN_DIR)."
rm -rf $REPAIRNATOR_RUN_DIR
#!/usr/bin/env bash

set -e

REPAIRNATOR_INITIALIZED=1

echo "Set environment variables"

INIT_SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

command -v docker >/dev/null 2>&1 || { echo >&2 "Repairnator require docker to be installed. Check it out at https://www.docker.com"; exit 1; }
command -v uuidgen >/dev/null 2>&1 || { echo >&2 "Repairnator requires uuidgen to be installed."; exit 1; }

echo "Read global configuration"
. $INIT_SCRIPT_DIR/../config/repairnator.cfg

echo "Read user configuration"
if [ -r ~/repairnator.cfg ]; then
    . ~/repairnator.cfg
fi

if [ -z "$RUN_ID_SUFFIX" ]; then
    export RUN_ID=`uuidgen`
else
    export RUN_ID=`uuidgen`_$RUN_ID_SUFFIX
fi

echo "This will run with the following RUN_ID: $RUN_ID"

source $SCRIPT_DIR/utils/create_structure.sh
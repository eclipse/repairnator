#!/usr/bin/env bash

set -e

if [ -z "$HOME_REPAIR" ] || [ ! -d $HOME_REPAIR ]; then
    echo "Create home for repairnator: $HOME_REPAIR"
    mkdir -p $HOME_REPAIR
fi

mkdir -p $ROOT_LOG_DIR
mkdir -p $ROOT_BIN_DIR
mkdir -p $ROOT_OUT_DIR

echo "Create output dir : $REPAIR_OUTPUT_PATH"
mkdir $REPAIR_OUTPUT_PATH

echo "Create log directory: $LOG_DIR"
mkdir $LOG_DIR
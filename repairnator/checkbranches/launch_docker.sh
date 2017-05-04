#!/usr/bin/env bash

if [ "$#" -ne 2 ]; then
    echo "Usage: ./launch_docker.sh <repository> <destination path>"
    exit 2
fi

touch $2
docker build -t checkbranches .
docker run -v $2:/tmp/outputcheck.txt -e REPOSITORY=$1 checkbranches:latest
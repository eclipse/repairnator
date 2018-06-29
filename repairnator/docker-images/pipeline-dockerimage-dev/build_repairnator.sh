#!/bin/bash

set -e

echo "Pipeline version:"
java -jar /root/repairnator-pipeline.jar -h 2> /dev/null || true # We don't want this call to fail the script
echo "Repairnator-pipeline jar file installed in /root directory"
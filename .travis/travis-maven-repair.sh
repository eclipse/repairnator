#!/bin/bash
# builds the maven-repair plugin

set -e

cd ./maven-repair

mvn test

cd ..

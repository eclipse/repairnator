#!/bin/bash

sudo docker rmi repairnator-pipeline
cd ../../..
mvn package -DskipTests
cp src/repairnator-pipeline/target/repairnator-pipeline-3.3-SNAPSHOT-jar-with-dependencies.jar src/docker-images/pipeline-dockerimage/repairnator-pipeline.jar
cd src/docker-images/pipeline-dockerimage

sudo docker build . -t repairnator-pipeline
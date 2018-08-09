# Repairnator

## Usage

The usage of Repairnator is described in [the dedicated documentation](/doc/usage).

## Content of the directory

This directory contains the different subfolders:
  - repairnator-*: are the different Repairnator modules
  - docker-images: contains the content of the different docker images produced for Repairnator
  - scripts: contains all the script to run Repairnator

## How does Repairnator work?

### Modules

Repairnator is decomposed in 6 different modules: 
  - repairnator-core: contains shared elements for all other elements, it can be also use to generate Google Spreadsheets Credentials
  - repairnator-scanner: scan automatically Travis Build and produce metrics and list of build ids (only for Bears)
  - repairnator-dockerpool: this part can be used to create a pool of docker containers to launch taking as input a list of build ids
  - repairnator-pipeline: it is the engine of Repairnator. Given a build id this part, compile, test and repair it, gathering data on it, and push on Github and on MongoDB
  - repairnator-realtime: inspects in realtime the builds from Travis (typically every minute)
  - repairnator-checkbranches: this part is used to validate efficiently a large set of data produced by Repairnator

Usage of each parts are detailed in their own Readme file.

### Process 

The following process is generally used.

First, `repairnator-pipeline` is built using shared element from `repairnator-core` and a docker image is created. 

If the user wants to follow and repair in realtime the builds procuced by Travis, then she should use `repairnator-realtime`.
The component is build using elements from `repairnator-core` and `repairnator-dockerpool`: the docker image previously created will be used automatically for each failing build.

Then if the user wants to repair scanned builds on a timerange, she should use `repairnator-scanner` and then `repairnator-dockerpool`.
First she built and launch `repairnator-scanner`, using a list of Github project as input, and a list of build ids will be produced.
Then she can build and launch `repairnator-dockerpool` against this list of build ids: the previously created docker image will be then used.

Those process can be automatically triggered using the scripts given in `scripts` directory.
  

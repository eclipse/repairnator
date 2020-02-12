# Repairnator


## Content of the directory

This directory contains the different subfolders:
  - docker-images: contains the content of the different docker images produced for Repairnator
  - scripts: contains all the script to run Repairnator
  - Maven modules
    - repairnator-core: contains shared elements for all other elements, it can be also use to generate Google Spreadsheets Credentials
    - repairnator-pipeline: it is the engine of Repairnator. Given a build id this part, compile, test and repair it, gathering data on it, and push on Github and on MongoDB
    - repairnator-realtime: inspects in realtime the builds from Travis (typically every minute)


## Build Process 

The following process is generally used.

First, `repairnator-pipeline` is built using shared element from `repairnator-core` and a docker image is created. 

If the user wants to follow and repair in realtime the builds procuced by Travis, then she should use `repairnator-realtime`.

Those processes can be automatically triggered using the scripts given in `scripts` directory.
  

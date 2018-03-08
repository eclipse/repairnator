[![Build Status](https://travis-ci.org/Spirals-Team/repairnator.svg?branch=master)](https://travis-ci.org/Spirals-Team/repairnator) [![Coverage Status](https://coveralls.io/repos/github/Spirals-Team/repairnator/badge.svg?branch=master)](https://coveralls.io/github/Spirals-Team/repairnator?branch=master)

# Repairnator: a program repair bot for continuous integration

Repairnator is a software development bot that automatically repairs build failures on Travis CI.
It scans failing Travis builds, tries to locally reproduce the failing build and tries to repair it with a program repair tool (eg Nopol or Astor). 

See [How to Design a Program Repair Bot? Insights from the Repairnator Project](https://hal.archives-ouvertes.fr/hal-01691496/document) (Simon Urli, Zhongxing Yu, Lionel Seinturier, Martin Monperrus). In Proceedings of 40th International Conference on Software Engineering, Track Software Engineering in Practice (SEIP), 2018.

```
@inproceedings{repairnator,
  TITLE = {{How to Design a Program Repair Bot? Insights from the Repairnator Project}},
  AUTHOR = {Urli, Simon and Yu, Zhongxing and Seinturier, Lionel and Monperrus, Martin},
  BOOKTITLE = {{ICSE 2018 - 40th International Conference on Software Engineering, Track Software Engineering in Practice (SEIP)}},
  PAGES = {1-10},
  YEAR = {2018},
  DOI = {10.1145/nnnnnnn.nnnnnnn},
}
```

## Quickstart

### Requirements

In order to run Repairnator with the provided scripts, you'll need the following components installed: 
  - git
  - docker
  - uuidgen utility tools (available for Mac & Linux)
 
You also need to get a Github API key: Go to [Github Personal Access Tokens](https://github.com/settings/tokens), and click on "Generate new token". 
You don't need to tick any box for Repairnator. Then just copy and keep the generated token somewhere safe.

### Setup Repairnator

All Repairnator scripts are located in the directory `repairnator/scripts`. 
The scripts use the configuration set in `repairnator/scripts/set_env_variable.sh`.

In order to use Repairnator: 
   1. clone this repository, 
   2. open in a file editor `repairnator/scripts/set_env_variable.sh`
   3. edit the file to specify the mandatory elements (you must add the Github Personal Access Token here)

### Launch Repairnator on a given Travis Build ID

From a Travis URL like this one: https://travis-ci.org/surli/failingProject/builds/350466198 you can retrieve a Build ID by taking the last part of the URL.
Here it is: `350466198`.

All you have to do, to launch Repairnator to reproduce and try fixing this build is then to go in `repairnator/scripts/` and launch `repair_buggy_build.sh` with the build ID as argument:

```bash
cd github/repairnator/repairnator/scripts
./repair_buggy_build.sh 350466198
```

The script will start a docker container to run Repairnator on your specified Build ID.

## How does Repairnator work?

RepairNator is decomposed in 4 different entities, which can be used independently of in interaction: 
  - repairnator-core: contains shared elements for all other elements, it can be also use to generate Google Spreadsheets Credentials
  - repairnator-scanner: as indicated by the name, this part can be used to scan automatically Travis Build and produce metrics and list of build ids
  - repairnator-dockerpool: this part can be used to create a pool of docker containers to launch, given a list of build ids
  - repairnator-pipeline: the main part of RepairNator, given a build id this part will try to compile, test and repair it, gathering data on it
  
Usage of each parts are detailed in their own Readme file.

## Content of the repository

This repository contains three sub projects:

  * [RepairNator](https://github.com/Spirals-Team/librepair/tree/master/repairnator) is the main program dedicated to this project: it can automatically scan large set of projects, detect failing builds, reproduce them and try to repair them using our tools
  * [travisFilter](https://github.com/Spirals-Team/librepair/tree/master/travisFilter) is a really small project intented to filter quickly set of Github project to detect if they're using Travis or not.
  * [sandbox](https://github.com/Spirals-Team/librepair/tree/master/sandbox) TODO

## Scripts

 `librepair/resources/clean_old_branches.sh` removes bad branches from https://github.com/Spirals-Team/librepair-experiments


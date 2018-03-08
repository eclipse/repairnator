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

## Content of the repository

This repository is organized as following:

  * [Repairnator](/tree/master/repairnator) is the main program dedicated to this project: it can automatically scan large set of projects, detect failing builds, reproduce them and try to repair them using our tools
  * [bears-usage](/tree/master/bears-usage) is a side project dedicated to gather data from repairnator.json files
  * [resources](/tree/master/resources) contains mainly data produced by Repairnator and scripts to retrieve those data. It also contain the schema of repairnator.json files.
  * [website](/tree/master/website) contains all data to produce repairnator website
  
Each directory contains its own Readme explaining its own internal organization.

## License

This project has been funded by InriaHub. The content of this repository is licensed under the AGPL terms. 


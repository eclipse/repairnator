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

## How does Repairnator work?

RepairNator is decomposed in 4 different entities, which can be used independently of in interaction: 
  - repairnator-core: contains shared elements for all other elements, it can be also use to generate Google Spreadsheets Credentials
  - repairnator-scanner: as indicated by the name, this part can be used to scan automatically Travis Build and produce metrics and list of build ids
  - repairnator-dockerpool: this part can be used to create a pool of docker containers to launch, given a list of build ids
  - repairnator-pipeline: the main part of RepairNator, given a build id this part will try to compile, test and repair it, gathering data on it
  
Usage of each parts are detailed in their own Readme file.
   
## How to use Repairnator?

On a linux machine, be sure to install Java 8, Maven 3.3.9, Docker and uuidgen tool (package uuid-runtime on Ubuntu).

Create a directory dedicated to repairnator called `librepair` and create in it the following folders:
  - bin
  - logs
  - scripts
  - github
  
Under github directory, clone this repository and then copy and paste `travis/travis-install.sh` bash file.
Launch it with the following command:

```
chmod +x travis-install.sh
./travis-install.sh
```

Then go back to `librepair` directory and launch the following commands:
```
chmod +x ./scripts/*.sh
./scripts/install_git_rebase_last.sh
```

Then create a file containing list of GitHub project names (one name per line) in file `scripts/project_list.txt`.

Then edit file `scripts/set_env_variable.sh` to put right information.

And finally you should be able to launch repairnator executing the following command:

```
./scripts/launch_repairnator.sh
```

You can also launch it with a list of build ids passed as argument, to skip the scanning process: 

```
./scripts/launch_repairnator.sh /path/to/file/with/build/ids
```

## Content of the repository

This repository contains three sub projects:

  * [RepairNator](https://github.com/Spirals-Team/librepair/tree/master/repairnator) is the main program dedicated to this project: it can automatically scan large set of projects, detect failing builds, reproduce them and try to repair them using our tools
  * [travisFilter](https://github.com/Spirals-Team/librepair/tree/master/travisFilter) is a really small project intented to filter quickly set of Github project to detect if they're using Travis or not.
  * [sandbox](https://github.com/Spirals-Team/librepair/tree/master/sandbox) TODO

## Scripts

 `librepair/resources/clean_old_branches.sh` removes bad branches from https://github.com/Spirals-Team/librepair-experiments


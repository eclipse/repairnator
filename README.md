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

In order to run Repairnator with the provided scripts, you'll need: 
  - git
  - docker
  - uuidgen utility tools (available for Mac & Linux)
  - a Github API key (Go to [Github Personal Access Tokens](https://github.com/settings/tokens), and click on "Generate new token".)
  
### Setup Repairnator

All Repairnator scripts are located in the directory `repairnator/scripts`. 
The scripts use the global configuration set in `repairnator/scripts/config/repairnator.cfg`.
You can define your own configuration in a file `repairnator.cfg` located in your home.

```bash
git clone https://github.com/Spirals-Team/repairnator/
cd repairnator

# edit the file to specify the mandatory elements (you must add the Github Personal Access Token here)
cp repairnator/scripts/config/repairnator.cfg ~
vi ~/repairnator.cfg
```

### Launch Repairnator on a given Travis Build ID

From a Travis URL like this one: https://travis-ci.org/surli/test-repairnator/builds/352395977 you can retrieve a Build ID by taking the last part of the URL.
Here it is: `352395977`.

All you have to do, to launch Repairnator to reproduce and try fixing this build is then to go in `repairnator/scripts/` and launch `repair_buggy_build.sh` with the build ID as argument:

```bash
# set $HOME_REPAIR and $GITHUB_OAUTH in repairnator/scripts/config/repairnator.cfg
cp repairnator/scripts/config/repairnator.cfg ~
vi ~/repairnator.cfg

cd repairnator/scripts

# start a docker container and run Repairnator on your specified Build ID.
./repair_buggy_build.sh 352395977

# find the container name
docker ps

# look at the logs
docker logs -f 849ef603b056 # is the name of the docker container

# When the docker container is done you can find logs and serialized files in the `$HOME_REPAIR/logs` path.
ls $HOME_REPAIR/logs
```

### Launch Repairnator to analyze and repair failing builds in real-time

You can launch Repairnator to analyze TravisCI builds in realtime and to repair failing ones.
First open in a file editor `repairnator/scripts/set_env_variable.sh` and edit the values under `Realtime scanner configuration` section:
  - `WHITELIST_PATH` and `BLACKLIST_PATH` should point on existing files: it can be empty files, or you can use those available in `repairnator/repairnator-realtime/src/main/resources`
  - `DURATION` is an optional value: if the value is left blank, the process will never stop; else it will last the specified duration (pay attention on the format, see: [https://en.wikipedia.org/wiki/ISO_8601#Durations](https://en.wikipedia.org/wiki/ISO_8601#Durations))

Then just run the script `launch_rtscanner.sh`.

For more information about the usage of Repairnator check out [this page](/doc/usage.md).

## Content of the repository

This repository is organized as following:

  * [doc](/doc) contains some documentation about Repairnator and its usage
  * [Repairnator](/repairnator) is the main program dedicated to this project: it can automatically scan large set of projects, detect failing builds, reproduce them and try to repair them using our tools
  * [bears-usage](/bears-usage) is a side project dedicated to gather data from repairnator.json files
  * [resources](/resources) contains mainly data produced by Repairnator and scripts to retrieve those data. It also contain the schema of repairnator.json files.
  * [website](/website) contains all data to produce repairnator website
  
Each directory contains its own Readme explaining its own internal organization.

## License

This project has been funded by InriaHub. The content of this repository is licensed under the MIT terms. 


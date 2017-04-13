# RepairNator

## What it is?

The first goal of RepairNator is to scan failing Travis builds, try to reproduce the failing build and to repair it. 
But it can also be used to scan builds looking for test fixes, in order to compute automatically metrics about those fixes. 

So the features of RepairNator are the following:
  - it scans builds from Travis API, creating list of build ids respecting some given contracts, and compute metrics on them
  - it tries to reproduce Maven Java builds by compiling them and launching test on them, it also aggregate data on them
  - it can push on a specific repository the data of reproduced builds
  - it automatically reproduce pull requests, and it's capable of searching for deleted commits using GitHub API
  - it push all gathered metrics on a Google Spreadsheets, but it can also produce JSON or CSV data files
  - it uses docker to run the builds on isolation

## How does it work?

RepairNator is decomposed in 4 different entities, which can be used independently of in interaction: 
  - repairnator-core: contains shared elements for all other elements, it can be also use to generate Google Spreadsheets Credentials
  - repairnator-scanner: as indicated by the name, this part can be used to scan automatically Travis Build and produce metrics and list of build ids
  - repairnator-dockerpool: this part can be used to create a pool of docker containers to launch, given a list of build ids
  - repairnator-pipeline: the main part of RepairNator, given a build id this part will try to compile, test and repair it, gathering data on it
  
Usage of each parts are detailed in their own Readme file.
   
## How to use it?

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



## Why "RepairNator"?
Because if the main objective of Terminator was "Seek and Destroy", the main goal of RepairNator is "Scan and Repair".
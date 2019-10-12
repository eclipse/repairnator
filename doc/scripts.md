# Scripts to run repairnator


**About repairnator.cfg**: All the scripts are using configuration given in `repairnator.cfg`.
This file is by default in `repairnator/scripts/config` but all values contained in the `$USER_HOME/repairnator.cfg` will override the default values.

For more details about the configuration, [read this documentation](repairnator-config.md).

## `launch_rtscanner.sh`

You can launch Repairnator to analyze Travis CI builds in realtime and to repair failing ones.

First open your `repairnator.cfg` config file (see above) and edit the values under `Realtime scanner configuration` section:
  - `DURATION` is an optional value: if the value is left blank, the process will never stop; else it will last the specified duration (pay attention on the format, see: [https://en.wikipedia.org/wiki/ISO_8601#Durations](https://en.wikipedia.org/wiki/ISO_8601#Durations))
  - `WHITELIST_PATH` and `BLACKLIST_PATH` can be left on the default value, or you can use the files available in `repairnator/repairnator-realtime/src/main/resources`
  
Then just run the script `launch_rtscanner.sh`.

* this generates build reproduction info and patches as local files in a folder named `logs/`
* in this default setup, no MongoDB is used, no email notification is done

## `repair_buggy_build.sh`

From a Travis URL like this one: https://travis-ci.org/surli/test-repairnator/builds/352395977 you can retrieve a Build ID by taking the last part of the URL.
Here it is: `352395977`.

All you have to do to launch Repairnator to reproduce and try fixing this build is then to go in `repairnator/scripts/` and launch `repair_buggy_build.sh` with the build ID as argument:

```bash
# set $HOME_REPAIR and $GITHUB_OAUTH in repairnator/scripts/config/repairnator.cfg
cp repairnator/scripts/config/repairnator.cfg ~
vi ~/repairnator.cfg

cd repairnator/scripts

# start a docker container and run Repairnator on your specified Build ID.
./repair_buggy_build.sh 352395977

# When the docker container is done you can find logs and serialized files in the `$HOME_REPAIR/logs` path.
ls $HOME_REPAIR/logs
```

## `launch_dockerpool.sh`

### Usage and argument

```
./launch_dockerpool.sh <list_of_builds>
```

`list_of_builds` is a mandatory argument. 
It's the path to an existing document which should contain a list of Travis Build IDs, one per line.

### Description

This script is used to repair a list of buggy builds by running them in docker containers.

### How it works

It will start a dockerpool and fill it with the list of build IDs read from the given argument.
Then it will use the [NB_THREADS](repairnator-config.md#nb_threads) options to launch as many docker containers as possible in parallel to repair the builds.

## `launch_repairnator.sh`

### Usage and argument
```
./launch_repairnator.sh [list_of_BuildIDs]
```

`list_of_BuildIDs` is an optional argument. 
It must be the path to an existing document. 
This document must contain a list of Travis Build IDs to repair, one per line.

### Description

This script is used to repair failing builds from the past. 

### How it works

With an argument, this script does exactly the same job as `launch_dockerpool.sh`.
Without an argument, it's a combination of `launch_scanner.sh` and `launch_dockerpool.sh`.

## `launch_checkbranches.sh`

### Usage and argument

```
./launch_checkbranches.sh <input branch names> <output result>
```

`input branch names` is a mandatory argument. It's the path to an existing file containing names of Git branches from the repository specified in [CHECK_BRANCH_REPOSITORY](repairnator-config.md#check_branch_repository).

`output result` is a mandatory argument. It's the path, not necessarily existing, of a file that will contain the result of the check.

### Description

This script will check the given branches to verify they are correct given the following criteria:
  - they contain the right number of commits,
  - the buggy commit is a failing test,
  - the fixing commit makes a successful build,
  - the json files respects the schema
  
### How it works

It launches a docker container for each branch provided in argument, using a pool of container and the [NB_THREADS](repairnator-config.md#nb_threads) option.

## `patched_builds.py`

This is a script that fetches data from a specified database that
follows the document specification described in chore. 

It will create a txt-file for each build in the collection `inspector`
with the travis-build URL, the github commit URL along with every
patch that was created for this specific build together with the
toolname that produced each patch.

The text file will have a html format and the following xpath queries
provide some interesting data for each file.

### Select all patches created by `<tool>`
`//[@title="tool"]`

### Select all patches
`//[@class="patch"]`

### Select travis-url
`//[@id="travis-url"]`

### Select commit-url
`//[@id="commit-url"]`

### Get all links
`//[@href]`

### Get all toolnames (may have duplicates)
`//@title`

# Querying the database

The current database contains a lot of interesting data, and below will be some examples of queries that will extract some of this data. The date is an example and may be replaced with dates you intend to look between.

## Number of builds with test-failures on travis

db.rtscanner.find({$and :[ {dateWatched:{$gte:ISODate("2018-01-01T00:00:00Z"),$lt:ISODate("2018-06-30T23:59:59Z")}},{status:"failed"}]}).count()

## Number of builds that were able to be reproduced by repairnator

db.inspector.find({$and :[{buildFinishedDate:{$gte:ISODate("2018-01-01T00:00:00Z"),$lt:ISODate("2018-06-30T23:59:59Z")}},{$or:[{status:"test failure"}, {status:"PATCHED"}]}]}).count()

## Number of builds where at least one patch was found

db.inspector.find({$and :[ {buildFinishedDate:{$gte:ISODate("2018-01-01T00:00:00Z"),$lt:ISODate("2018-06-30T23:59:59Z")}},{status:"PATCHED"}]}).count()


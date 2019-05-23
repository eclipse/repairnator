# Scripts
We detail here all the scripts that are available in `repairnator/scripts` and their usage.

## About repairnator.cfg

All the scripts are using configuration given in `repairnator.cfg`.
This file is by default in `repairnator/scripts/config` but all values contained in the `$USER_HOME/repairnator.cfg` will override the default values.

For more details about the configuration, [read this documentation](repairnator-config.md).

## repair_buggy_build.sh

### Usage and argument
```
./repair_buggy_build.sh <BUILDID>
```

`BUILDID` is a mandatory argument. It specifies the Travis CI build ID that is failing and that we want to repair. 

### Description

This script is used to repair a single buggy build.

### How it works

It will start a single instance of repairnator-pipeline in a docker container which attempts to
repair the build specified in argument.

## launch_rtscanner.sh

### Usage and argument
```
./launch_rtscanner.sh
```

### Description

This script is used to launch Repairnator to repair every failing builds coming from Travis CI in live.

### How it works

Starts a scanner which looks for java failing builds on Travis CI in realtime. 
For each it finds it will start a docker container with an instance of
repairnator-pipeline that tries to repair the build. It will run for
as long as specified by the [DURATION option](repairnator-config.md#duration).

## launch_scanner.sh

### Usage and argument

```
./launch_scanner.sh
```

### Description

This script is used to retrieve failing builds from Travis CI in the past. 

### How it works

It will use the [REPAIR_PROJECT_LIST_PATH](repairnator-config.md#repair_project_list_path) option
to get a list of projects to scan.

Once this list has been read, repairnator will run a scanner to find
failing builds. 

The timespan in which the scanner will look is specified by either [SCANNER_NB_HOURS](repairnator-config.md#scanner_nb_hours) option, 
or by the two options [LOOK_FROM_DATE](repairnator-config.md#look_from_date) and
[LOOK_TO_DATE](repairnator-config.md#look_to_date) together.

## launch_dockerpool.sh

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

## launch_repairnator.sh

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

## launch_checkbranches.sh

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

# patched_builds.py

This is a script that fetches data from a specified database that
follows the document specification described in chore. 

It will create a txt-file for each build in the collection `inspector`
with the travis-build URL, the github commit URL along with every
patch that was created for this specific build together with the
toolname that produced each patch.

# Some info about the mongodb data

The current database contains a lot of interesting data, and below will be some examples of queries that will extract some of this data. The date is an example and may be replaced with dates you intend to look between.

## Number of builds with test-failures on travis

db.rtscanner.find({$and :[ {dateWatched:{$gte:ISODate("2018-01-01T00:00:00Z"),$lt:ISODate("2018-06-30T23:59:59Z")}},{status:"failed"}]}).count()

## Number of builds that were able to be reproduced by repairnator

db.inspector.find({$and :[{buildFinishedDate:{$gte:ISODate("2018-01-01T00:00:00Z"),$lt:ISODate("2018-06-30T23:59:59Z")}},{$or:[{status:"test failure"}, {status:"PATCHED"}]}]}).count()

## Number of builds where at least one patch was found

db.inspector.find({$and :[ {buildFinishedDate:{$gte:ISODate("2018-01-01T00:00:00Z"),$lt:ISODate("2018-06-30T23:59:59Z")}},{status:"PATCHED"}]}).count()

# Repairnator.cfg

This documentation details the different elements of Repairnator configuration contained in `repairnator.cfg`.
The standard location of this configuration file is `scripts/config`, however user might create a `repairnator.cfg` file in their `$HOME` directory:
all values will be then be overridden by those from the `$HOME` directory.

## Standard configuration

All the following is part of the standard configuration.

### Common attributes

Those attributes are commons for all scripts.

#### GITHUB_OAUTH

This attribute is mandatory. 

It is used to get access to Github API. 
In order to create the token go to the following page: https://github.com/settings/tokens and create a new token by selecting `public_repo`.

#### HOME_REPAIR

This attribute is mandatory.

It defines the home directory for all operations done by Repairnator's scripts. If the directory does not exist, Repairnator will try to create it.

Its default value is `$HOME/repairnator` where `$HOME` is the user home directory.

### Pipeline attributes

Those attributes are used mainly for the Repairnator pipeline, the main part of Repairnator, but might also be common to other parts.

#### GITHUB_USERNAME

This attribute is optional.

Repairnator might commit information, like the patches it will create, or even information about reproducing bugs (see [#PUSH_URL](#PUSH_URL)).
This attribute specify the name of the committer. Don't forget to use quotes if you want to put space in its value: `GITHUB_USERNAME="John Doe"`.

Its default value is `repairnator`.

#### GITHUB_USEREMAIL

This attribute is optional.

As for the previous one, here the information concerns the committer email. 

Its default value is `noreply@github.com` which is the default Github email for hidden emails.

#### MONGODB_HOST

This attribute is optional.

Repairnator produces a lot of information. The default behaviour is to serialize all information as CSV and JSON files. 
However a good practice is to store them in a MongoDB database in order to be able to compute data with them.
This attribute specify the host where a MongoDB is available for Repairnator. The format of the value is: `mongodb://user:password@domain:port`.

Its default value is empty.

#### MONGODB_NAME

This attribute is optional.

As for the previous one, if the user wants to use a MongoDB she also needs to specify the database name with this attribute.

Its default value is empty.

#### PUSH_URL

This attribute is optional.

Another way to store data, is to automatically push the content of the repository for the reproduced bugs.
This attribute aims at specifying a Github repository in which Repairnator will push a new branch for each new failing build it manages to reproduce.
More specifically it will push branches with the following format: `repositoryUser-repositoryName-TravisBuildID-date-hours`. 
The content of this branch will be content of the repository with the reproducing builds and some Repairnator logs and maybe patches.

This attribute takes the following format: `https://github.com/user/repo`. It only accepts to push on Github, and the repository must be writable by the owner of the `GITHUB_OAUTH` token.

Its default value is empty.

#### SMTP_SERVER

This attribute is optional. 

A Repairnator user might want to receive email notification when a patch is created or an error occured on Repairnator.
In order to do so, Repairnator should use a SMTP server. This attribute allow to specify which server to use.
For now Repairnator only supports servers without authentication.

Its default value is empty.

#### NOTIFY_TO

This attribute is optional.

If a user wants to receive notification (see previous attribute), she should specify her email address on this attribute.
Multiple address can be given separated by a comma: e.g. `NOTIFY_TO=john.doe@mailserver.com,jane.doe@anothersrv.fr`.

Its default value is empty.

#### NOTIFY_ENDPROCESS

This attribute is a switch.

If switch on (meaning, if it is set to 1), the end of dockerpool and scanner processes will send a notification.
This only works if `SMTP_SERVER` and `NOTIFY_TO` are filled.

Its default value is 0 (off).

#### RUN_ID_SUFFIX

This attribute is optional.

When running any scripts on Repairnator, a UUID is created which we call the `RUN_ID`. This UUID is used to distinguish between different invocations of Repairnator.
However, as it is a generated UUID it might be difficult to remember and to look back on a database.
To simplify this job, we provide with this attribute a way to add a suffix to this generated UUID: we guarantee the uniquess of the UUID, but we simplify its usage in the DB.

Its default value is empty.

#### REPAIR_TOOLS

This attribute is mandatory for the pipeline.

The main goal of Repairnator is to use various program repair tools to try repairaing failing builds from Travis CI.
However, for one repair tools, different strategies might be available, and a user do not necessarily wants to run all repair tools and all strategies each time.
This attribute allows to select the different repair tools available in Repairnator. For more information about the available repair tools, [have a look on this page](repair-tools.md).
Several repair tools can be selected, separated by a comma.


Its default value is `NopolAllTests,AstorJMut,NPEFix`.


#### CREATE_PR

This attribute is a switch. 

When switch on (set to 1), Repairnator will try to create automatically a pull request, for the patch it manages to generate.

Its default value is 0 (off).

### Scanner attributes

Those attributes are used for the scanner.

#### REPAIR_PROJECT_LIST_PATH

This attribute is mandatory for the scanner.

The Repairnator Scanner uses a list of project slugs (a slug is composed by a Github username and a repository name, the slug of Repairnator is then `Spirals-Team/repairnator`) as input for scanning Travis CI builds.
This attribute gives the path of the list of projects to scan. This list should be a textual file, with a slug name by line. The given path should rely on an existing file.

Its default value is `$HOME_REPAIR/project_list.txt`. More explanation about `HOME_REPAIR` are given on the beginning of this page.

#### SCANNER_NB_HOURS

This attribute is mandatory if the following ones are left empty or commented.

The Repairnator Scanner works in two different ways:
  1. it might scan a given period of time;
  2. or it might look X hours before the current time
  
This attribute is used for the second proposal: it takes an integer as value and it will look the given number of hours before the current time to take the failing builds.

Its default value is `1`.  

#### LOOK_FROM_DATE

This attribute is mandatory if the previous one is left empty or commented.

This attribute is used for scanning a given period of time. It gives the starting point for this period of time. 
It only takes a day on the format `dd/MM/yyyy` and it will start looking at `00:00:00` of that given day.

Its default value is empty.

#### LOOK_TO_DATE

This attribute is mandatory if the previous one is used.

This attribute is used for scanning a given period of time. It gives the ending point for this period of time.
It only takes a day on the format `dd/MM/yyyy` and it will end looking at `23:59:59` of that given day.

Its default value is empty.

### Docker pool attributes

Those attributes are used for the dockerpool of Repairnator.

#### NB_THREADS

This attribute is mandatory for dockerpool.

This attribute is an integer which defines the number of docker containers that will be running in parallel by Repairnator.
This number should be changed according to the hardware capabilities of the machine which runs Repairnator.

Its default value is `4`.

#### DAY_TIMEOUT

This attribute is mandatory for dockerpool.

This attribute defines when a running docker container should be forced to stop.
It's an integer which defines the number of days that the container can run at most.

Its default value is `1`.

### Realtime scanner attributes

Those attributes are used by the Repairnator realtime scanner (or RTScanner)

#### WHITELIST_PATH

This attribute is mandatory for RTScanner.

This attribute defines the path for a whitelist of repositories to scan. 
This whitelist is composed by Github ID (and not slug) of Github Repositories that are considered usable by Repairnator.
Each ID is on a new line.
This attribute might point to a missing file: the file will then be created at the given path.

Its default value is `$HOME_REPAIR/whitelist.txt` (for more information about `$HOME_REPAIR` look at the beginning of this page).

#### BLACKLIST_PATH

This attribute is mandatory for RTScanner.

This attribute defines the path for a blacklist of repositories to scan.
This list works the same than for the previous attribute, but to record blacklisted repository that won't be scanned.
Here again, the attribute might point to a missing file: the file will then be created at the given path.

Its default value is `$HOME_REPAIR/blacklist.txt` (for more information about `$HOME_REPAIR` look at the beginning of this page).

#### DURATION

This attribute is optional.

This attribute defines the duration of the RTScanner before it stops.
The duration is given using [the ISO-8601 duration format](https://en.wikipedia.org/wiki/ISO_8601#Durations).
If no duration is given, the RTScanner never stop: it can be used to launch it in daemon mode.

Its default value is `PT10m` (10 minutes).

#### JOB_SLEEP_TIME

This attribute is mandatory for RTScanner.

The RTScanner scans Travis CI regularly to detect newly created jobs from whitelisted repository.
This attribute defines the duration in second between each request to Travis CI API for getting new jobs.
It takes as input an integer which defines the number of seconds between each request.

Its default value is `10`.

#### BUILD_SLEEP_TIME

This attribute is mandatory for RTScanner.

When the RTScanner detects a newly created job from Travis CI, it will wait until the complete build of this job is finished.
This attribute defines the duration between each refresh of information of a build using Travis CI API. 
It takes an integer which defines the number of seconds between each request to refresh the information.

Its default value is `10`.

#### LIMIT_INSPECTED_BUILDS

This attribute is mandatory for RTScanner.

The RTScanner cannot inspect at the same time all builds from Travis CI. 
This attribute defines the maximum number of builds to examine in parallel.

Its default value is `100`.

### Checkbranch attributes

#### CHECK_BRANCH_REPOSITORY

This attribute is mandatory for checkbranch.

Checkbranch will check the branches of a repository to detect if they're correct, regarding several properties of Repairnator.
This attribute defines which repository should be checked.

Its default value is empty.

#### HUMAN_PATCH

This attribute is a switch.

Checkbranch will check that the branches of a repository contain a commit representing a bug.
It can also check that the branch contains a commit representing a human patch.
When this attribute is switched on (if it set to 1), it will check this property.

Its default value is `0` (off).

## Advanced configuration

All the following is part of the advanced configuration: it should only be changed by advanced users.

### Versions

This section contains the attributes to change the version of Repairnator to use.

#### SCANNER_VERSION

This attribute is mandatory.

User can indicate here the number of a version to use or `LATEST` in uppercase to use the latest release available.
It's possible to use latest snapshot, only by indicating its number here.
This attribute concerns only the version of the Repairnator scanner.

Its default value is `LATEST`.

#### DOCKERPOOL_VERSION

This attribute is mandatory.

User can indicate here the number of a version to use or `LATEST` in uppercase to use the latest release available.
It's possible to use latest snapshot, only by indicating its number here.
This attribute concerns only the version of the Repairnator dockerpool.

Its default value is `LATEST`.

#### REALTIME_VERSION

This attribute is mandatory.

User can indicate here the number of a version to use or `LATEST` in uppercase to use the latest release available.
It's possible to use latest snapshot, only by indicating its number here.
This attribute concerns only the version of the Repairnator RTScanner.

Its default value is `LATEST`.

#### CHECKBRANCHES_VERSION

This attribute is mandatory.

User can indicate here the number of a version to use or `LATEST` in uppercase to use the latest release available.
It's possible to use latest snapshot, only by indicating its number here.
This attribute concerns only the version of the Repairnator checkbranches.

Its default value is `LATEST`.

#### PIPELINE_VERSION

This attribute is mandatory.

User can indicate here the number of a version to use or `latest` in **lowercase** to use the latest version available.
Be careful, in contrary with the previous ones, it concerns here a docker image: then the latest version really means the latest, so the version "in development".
To use a release version, one has to specify the number of the specific release to use.

Its default value is `latest`.

### Docker tags

This sections describes the name of docker images to use in the different scripts.

#### DOCKER_TAG

This attribute is mandatory.

It defines the name of the docker image to use for Repairnator pipeline, with the associated tag to use (see the previous `PIPELINE_VERSION`).

Its default value is `spirals/repairnator:$PIPELINE_VERSION`.


#### DOCKER_TAG_BEARS

This attribute is mandatory for BEARS.

It defines the name of the docker image to use for Repairnator pipeline when using BEARS, and the associated tag to use (see the previous `PIPELINE_VERSION`).

Its default value is `spirals/repairnator:$PIPELINE_VERSION`.

#### DOCKER_CHECKBRANCHES_TAG

This attribute is mandatory for using checkbranch.

It defines the name of the docker image to use for Repairnator checkbranch, and the associated tag to use.

Its default value is `spirals/checkbranches:latest`.

#### DOCKER_CHECKBRANCHES_TAG_BEARS

This attribute is mandatory for using BEARS checkbranch.

It defines the name of the docker image to use for Repairnator BEARS checkbranch, and the associated tag to use.

Its default value is `spirals/checkbranches:latest`.

### Root paths

This section defines the different main paths used in the scripts. 
All paths must end with a `/`.

#### ROOT_LOG_DIR

This attribute is mandatory.

It defines the main directory for containing the logs produced by Repairnator. 
The directory does not need to exist: it will be created if it does not.

Its default value is `$HOME_REPAIR/logs/` (see the beginning of this page for more info about `HOME_REPAIR`).

#### ROOT_OUT_DIR

This attribute is mandatory.

It defines the main directory for containing the outputs produced by Repairnator.
The directory does not need to exist: it will be created if it does not.

Its default value is `$HOME_REPAIR/out/` (see the beginning of this page for more info about `HOME_REPAIR`).

#### ROOT_BIN_DIR

This attribute is mandatory.

It defines the main directory for containing the binaries used by Repairnator.
The directory does not need to exist: it will be created if it does not.

Its default value is `$HOME_REPAIR/bin/` (see the beginning of this page for more info about `HOME_REPAIR`).

### Binary paths

This section defines the complete path of each binaries which might be used by the Repairnator scripts.

#### REPAIRNATOR_RUN_DIR

This attribute is mandatory.

It defines the temporary directory where to put the binaries used for a script.
This directory will be created automatically and deleted at the end of the script.

Its default value is `$ROOT_BIN_DIR\`date "+%Y-%m-%d_%H%M"\``.

#### REPAIRNATOR_SCANNER_DEST_JAR

This attribute is mandatory.

It defines the absolute path of the jar used for Repairnator scanner.

Its default value is `$REPAIRNATOR_RUN_DIR/repairnator-scanner.jar`.

#### REPAIRNATOR_DOCKERPOOL_DEST_JAR

This attribute is mandatory.

It defines the absolute path of the jar used for Repairnator dockerpool.

Its default value is `$REPAIRNATOR_RUN_DIR/repairnator-dockerpool.jar`.

#### REPAIRNATOR_CHECKBRANCHES_DEST_JAR

This attribute is mandatory.

It defines the absolute path of the jar used for Repairnator checkbranches.

Its default value is `$REPAIRNATOR_RUN_DIR/repairnator-checkbranches.jar`.

#### REPAIRNATOR_REALTIME_DEST_JAR

This attribute is mandatory.

It defines the absolute path of the jar used for Repairnator RTScanner.

Its default value is `$REPAIRNATOR_RUN_DIR/repairnator-realtime.jar`.

### Other paths

This section defines the other paths used by Repairnator.

#### REPAIR_OUTPUT_PATH

This attribute is mandatory.

It defines the final path of the output directory for a specific run of a script.

Its default value is `$ROOT_OUT_DIR\`date "+%Y-%m-%d_%H%M%S"\``.

#### LOG_DIR

This attribute is mandatory.

It defines the final path of the log directory for a specific run of a script.

Its default value is `$ROOT_LOG_DIR\`date "+%Y-%m-%d_%H%M%S"\``.

#### DOCKER_LOG_DIR=$LOG_DIR # Log directory for docker containers (most of the time, it should be the same as LOG_DIR, but sometimes if you use distant host, e.g. g5k you need to specify another value)

This attribute is mandatory.

It defines the log directory for the docker containers exclusively.
Most of the time, the same directory as `$LOG_DIR` should be used, but for some usage like when containers are running on multiple machines, etc. a specific configuration could be used.

Its default value is `$LOG_DIR`.

#### M2_HOME

This attribute is mandatory when executing repairnator-pipeline outside a docker container.

This attribute defines the path of maven home (and not of M2 repository!). 

Its default value is `$MAVEN_HOME`.

### Switches

This section defines all the other switches that can be used

#### BEARS_MODE

This attribute is a switch.

If switched on (set to 1), Repairnator will be used in BEARS mode.

Its default value is `0` (off).

#### BEARS_FIXER_MODE

This attribute is a switch.

BEARS can be used to detect failing builds followed by passing builds, or passing builds followed by passing builds with a test showing a previous bug.
We name those pairs of builds **failing passing** and **passing passing**.
This attribute takes as value: `failing_passing`, `passing_passing`, or `both`. The latter means that it will try to detect both kind of pairs of builds.

Its default value is `both`.

#### BEARS_DELIMITER=1 # When set to 1, the scanner writes different files for the different kind of statuses.

This attribute is a switch.

If switched on (set to 1), the scanner will write a different file for each kind of pair it has been detected (failing passing or passing passing).
This allow to treat them differently afterwards.

Its default value is `1` (on).

#### SKIP_LAUNCH_REPAIRNATOR

This attribute is a switch.

If switched on (set to 1), the dockerpool will be skipped in some scripts. Note that this option might be overridden in some scripts.

Its default value is `0` (off).

#### CREATE_OUTPUT_DIR=0 # Use specifically for grid5000: allow to create a subdirectory to contain logs/serialization of docker containers

This attribute is a switch.

This attribute is specific for using container on multiple machines such as when using Grid 5000.
When switched on (set to 1), it allows the creation of a subdirectory to contain logs and outputs of docker containers.

Its default value is `0` (off).
 
#### SKIP_DELETE=0 # Set to 1 to skip the deletion of docker containers

This attribute is a switch.

When switched on (set to 1), this attribute allows to skip the deletion of docker containers.
This switch might be useful in case of debugging.

Its default value is `0` (off).

### Optional java arguments

#### JAVA_OPTS= # optional arguments for java such as Xmx

This attribute is optional.

It allows to gives specify arguments to the JVM such as specifying the memory allocated with `Xmx`. 

Its default value is empty.
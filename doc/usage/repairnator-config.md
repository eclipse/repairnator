# Repairnator.cfg

This documentation details the different elements of `repairnator.cfg`.

## Common attributes

Those attributes are commons for all scripts.

### GITHUB_OAUTH

This attribute is mandatory. 

It is used to get access to Github API. 
In order to create the token go to the following page: https://github.com/settings/tokens and create a new token by selecting `public_repo`.

### HOME_REPAIR

This attribute is mandatory.

It defines the home directory for all operations done by Repairnator's scripts. If the directory does not exist, Repairnator will try to create it.

Its default value is `$HOME/repairnator` where `$HOME` is the user home directory.

## Pipeline attributes

Those attributes are used mainly for the Repairnator pipeline, the main part of Repairnator, but might also be common to other parts.

### GITHUB_USERNAME

This attribute is optional.

Repairnator might commit information, like the patches it will create, or even information about reproducing bugs (see [#PUSH_URL](#PUSH_URL)).
This attribute specify the name of the committer. Don't forget to use quotes if you want to put space in its value: `GITHUB_USERNAME="John Doe"`.

Its default value is `repairnator`.

### GITHUB_USEREMAIL

This attribute is optional.

As for the previous one, here the information concerns the committer email. 

Its default value is `noreply@github.com` which is the default Github email for hidden emails.

### MONGODB_HOST

This attribute is optional.

Repairnator produces a lot of information. The default behaviour is to serialize all information as CSV and JSON files. 
However a good practice is to store them in a MongoDB database in order to be able to compute data with them.
This attribute specify the host where a MongoDB is available for Repairnator. The format of the value is: `mongodb://user:password@domain:port`.

Its default value is empty.

### MONGODB_NAME

This attribute is optional.

As for the previous one, if the user wants to use a MongoDB she also needs to specify the database name with this attribute.

Its default value is empty.

### PUSH_URL

This attribute is optional.

Another way to store data, is to automatically push the content of the repository for the reproduced bugs.
This attribute aims at specifying a Github repository in which Repairnator will push a new branch for each new failing build it manages to reproduce.
More specifically it will push branches with the following format: `repositoryUser-repositoryName-TravisBuildID-date-hours`. 
The content of this branch will be content of the repository with the reproducing builds and some Repairnator logs and maybe patches.

This attribute takes the following format: `https://github.com/user/repo`. It only accepts to push on Github, and the repository must be writable by the owner of the `GITHUB_OAUTH` token.

Its default value is empty.

### SMTP_SERVER

This attribute is optional. 

A Repairnator user might want to receive email notification when a patch is created or an error occured on Repairnator.
In order to do so, Repairnator should use a SMTP server. This attribute allow to specify which server to use.
For now Repairnator only supports servers without authentication.

Its default value is empty.

### NOTIFY_TO

This attribute is optional.

If a user wants to receive notification (see previous attribute), she should specify her email address on this attribute.
Multiple address can be given separated by a comma: e.g. `NOTIFY_TO=john.doe@mailserver.com,jane.doe@anothersrv.fr`.

Its default value is empty.

### NOTIFY_ENDPROCESS

This attribute is a switch.

If switch on (meaning, if it is set to 1), the end of dockerpool and scanner processes will send a notification.
This only works if `SMTP_SERVER` and `NOTIFY_TO` are filled.

Its default value is 0 (off).

### RUN_ID_SUFFIX

This attribute is optional.

When running any scripts on Repairnator, a UUID is created which we call the `RUN_ID`. This UUID is used to distinguish between different invocations of Repairnator.
However, as it is a generated UUID it might be difficult to remember and to look back on a database.
To simplify this job, we provide with this attribute a way to add a suffix to this generated UUID: we guarantee the uniquess of the UUID, but we simplify its usage in the DB.

Its default value is empty.

### REPAIR_TOOLS

This attribute is mandatory for the pipeline.

The main goal of Repairnator is to use various program repair tools to try repairaing failing builds from Travis CI.
However, for one repair tools, different strategies might be available, and a user do not necessarily wants to run all repair tools and all strategies each time.
This attribute allows to select the different repair tools available in Repairnator. For more information about the available repair tools, [have a look on this page](repair-tools.md).
Several repair tools can be selected, separated by a comma.


Its default value is `NopolAllTests,AstorJMut,NPEFix`.

## Scanner attributes

Those attributes are used for the scanner.

### REPAIR_PROJECT_LIST_PATH

This attribute is mandatory for the scanner.

The Repairnator Scanner uses a list of project slugs (a slug is composed by a Github username and a repository name, the slug of Repairnator is then `Spirals-Team/repairnator`) as input for scanning Travis CI builds.
This attribute gives the path of the list of projects to scan. This list should be a textual file, with a slug name by line. The given path should rely on an existing file.

Its default value is `$HOME_REPAIR/project_list.txt`. More explanation about `HOME_REPAIR` are given on the beginning of this page.

### SCANNER_NB_HOURS

This attribute is mandatory if the following ones are left empty or commented.

The Repairnator Scanner works in two different ways:
  1. it might scan a given period of time;
  2. or it might look X hours before the current time
  
This attribute is used for the second proposal: it takes an integer as value and it will look the given number of hours before the current time to take the failing builds.

Its default value is `1`.  

### LOOK_FROM_DATE

This attribute is mandatory if the previous one is left empty or commented.

This attribute is used for scanning a given period of time. It gives the starting point for this period of time. 
It only takes a day on the format `dd/MM/yyyy` and it will start looking at `00:00:00` of that given day.

Its default value is empty.

### LOOK_TO_DATE

This attribute is mandatory if the previous one is used.

This attribute is used for scanning a given period of time. It gives the ending point for this period of time.
It only takes a day on the format `dd/MM/yyyy` and it will end looking at `23:59:59` of that given day.

Its default value is empty.

#### Docker pool configuration

NB_THREADS=4 # Number of concurrent docker container to run
DAY_TIMEOUT=1 # Global timeout to stop the docker execution

#### Realtime scanner configuration

WHITELIST_PATH=$HOME_REPAIR/whitelist.txt # Path of the whitelist of projects (mandatory but the file does not have to exist)
BLACKLIST_PATH=$HOME_REPAIR/blacklist.txt # Path of the blacklist (mandatory but the file does not have to exist)
DURATION=PT10m # Duration execution of the process on the ISO-8601 duration format: PWdTXhYmZs (e.g. PT1h for 1 hour)
JOB_SLEEP_TIME=10 # Sleep time in seconds for requesting /job endpoint in Travis
BUILD_SLEEP_TIME=10 # Sleep time in seconds for refreshing builds status
LIMIT_INSPECTED_BUILDS=100 # Maximum number of builds under inspection

#### Checkbranch configuration

CHECK_BRANCH_REPOSITORY= # Repository to use for check branches script
HUMAN_PATCH=0 # Test the human patch for check branches ?

##### ADVANCED CONFIGURATION

# Change the following configuration only if you know exactly what you're doing.

### Versions

SCANNER_VERSION=LATEST
DOCKERPOOL_VERSION=LATEST
REALTIME_VERSION=LATEST
PIPELINE_VERSION=latest
CHECKBRANCHES_VERSION=LATEST

### Docker tags
DOCKER_TAG=spirals/repairnator:$PIPELINE_VERSION # Tag of the docker image to use for pipeline
DOCKER_TAG_BEARS=spirals/bears:$PIPELINE_VERSION # Tag of the docker image to use for pipeline when running on Bears mode
DOCKER_CHECKBRANCHES_TAG=surli/checkbranches:latest # Tag of the docker image to use for checkbranches
DOCKER_CHECKBRANCHES_TAG_BEARS=fermadeiral/checkbranches:latest # Tag of the docker image to use for checkbranches when running on Bears mode

### Root pathes

ROOT_LOG_DIR=$HOME_REPAIR/logs/ # The directory will be created if it does not exist
ROOT_OUT_DIR=$HOME_REPAIR/out/ # The directory will be created if it does not exist
ROOT_BIN_DIR=$HOME_REPAIR/bin/ # The directory will be created if it does not exist

### Binary pathes
REPAIRNATOR_RUN_DIR=$ROOT_BIN_DIR`date "+%Y-%m-%d_%H%M"` # Where to put executables used (will be created automatically and deleted)
REPAIRNATOR_SCANNER_DEST_JAR=$REPAIRNATOR_RUN_DIR/repairnator-scanner.jar
REPAIRNATOR_DOCKERPOOL_DEST_JAR=$REPAIRNATOR_RUN_DIR/repairnator-dockerpool.jar
REPAIRNATOR_CHECKBRANCHES_DEST_JAR=$REPAIRNATOR_RUN_DIR/repairnator-checkbranches.jar
REPAIRNATOR_REALTIME_DEST_JAR=$REPAIRNATOR_RUN_DIR/repairnator-realtime.jar

### Other pathes
REPAIR_OUTPUT_PATH=$ROOT_OUT_DIR`date "+%Y-%m-%d_%H%M%S"` # Where to put output of repairnator
LOG_DIR=$ROOT_LOG_DIR`date "+%Y-%m-%d_%H%M%S"` # Log directory: it will contain several logs and serialized files
DOCKER_LOG_DIR=$LOG_DIR # Log directory for docker containers (most of the time, it should be the same as LOG_DIR, but sometimes if you use distant host, e.g. g5k you need to specify another value)

M2_HOME=$MAVEN_HOME # Path to the maven home: this value is only used when executing directly repairnator-pipeline.jar (outside a docker container)

### Switches

BEARS_MODE=0 # Set 1 to use the bears mode
BEARS_FIXER_MODE=both # Possible values are both, failing_passing, passing_passing or both
BEARS_DELIMITER=1 # When set to 1, the scanner writes different files for the different kind of statuses.
SKIP_LAUNCH_REPAIRNATOR=0 # If set to 1, skip the launch of docker pool: it will only launch the scanner. Note that this option might be overriden in some scripts.
CREATE_OUTPUT_DIR=0 # Use specifically for grid5000: allow to create a subdirectory to contain logs/serialization of docker containers
SKIP_DELETE=0 # Set to 1 to skip the deletion of docker containers

### Optional java arguments
JAVA_OPTS= # optional arguments for java such as Xmx

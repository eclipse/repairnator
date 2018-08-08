# Jars usage

This page describe the usage of all modules of Repairnator. All modules are located under [repairnator directory](/repairnator).

## Pipeline

### Requirements

The following command line tools must be installed on your machine:
  - Oracle Java: some dependencies need tools.jar in the classpath
  - cloc: we compute some metrics with cloc
  - git: to apply some git commands
  - z3: a constraint solver. The executables are available in [pipeline test resources](/repairnator/repairnator-pipeline/src/test/resources/z3)

### Usage

```bash
Usage: java <repairnator-pipeline name> [option(s)]

Options: 

  [-h|--help]

  [-d|--debug]

  [--runId <runId>]
        Specify the run id for this launch.

  [--bears]
        This mode allows to use repairnator to analyze pairs of bugs and
        human-produced patches.

  [(-o|--output) <output>]
        Specify path to output serialized files

  [--dbhost <mongoDBHost>]
        Specify mongodb host.

  [--dbname <mongoDBName>]
        Specify mongodb DB name. (default: repairnator)

  [--smtpServer <smtpServer>]
        Specify SMTP server to use for Email notification

  [--notifyto notifyto1,notifyto2,...,notifytoN ]
        Specify email addresses to notify

  [--pushurl <pushUrl>]
        Specify repository URL to push data.

  (-b|--build) <build>
        Specify the build id to use.

  [(-n|--nextBuild) <nextBuild>]
        Specify the next build id to use (only in BEARS mode). (default: -1)

  [--z3 <z3>]
        Specify path to Z3 (default: ./z3_for_linux)

  [(-w|--workspace) <workspace>]
        Specify a path to be used by the pipeline at processing things like to
        clone the project of the build id being processed (default: ./workspace)

  --ghOauth <ghOauth>
        Specify oauth for Github use

  [--projectsToIgnore <projectsToIgnore>]
        Specify the file containing a list of projects that the pipeline should
        deactivate serialization when processing builds from. (default:
        ./projects_to_ignore.txt)

Please note that the GITHUB_OAUTH environment variables must be set.
The environment variable M2_HOME should be set and refer to the path of your maven home installation.
For using Nopol, you must add tools.jar in your classpath from your installed jdk
``` 

### Dockerpool

This module located at [repairnator-dockerpool](/repairnator/repairnator-dockerpool) aims at managing docker containers of the Repairnator [pipeline](#pipeline).
This module takes as input a list of failing build ids to repair by using the [pipeline](#pipeline). This list of failing build ids might be produced by the [scanner](#scanner).


```bash
Usage: java <repairnator-dockerpool name> [option(s)]

Options: 

  [-h|--help]

  [-d|--debug]

  [--runId <runId>]
        Specify the run id for this launch.

  [--bears]
        This mode allows to use repairnator to analyze pairs of bugs and
        human-produced patches.

  (-i|--input) <input>
        Specify the input file containing the list of build ids.

  (-o|--output) <output>
        Specify where to put serialized files from dockerpool

  [--dbhost <mongoDBHost>]
        Specify mongodb host.

  [--dbname <mongoDBName>]
        Specify mongodb DB name. (default: repairnator)

  [--notifyEndProcess]
        Activate the notification when the process ends.

  [--smtpServer <smtpServer>]
        Specify SMTP server to use for Email notification

  [--notifyto notifyto1,notifyto2,...,notifytoN ]
        Specify email addresses to notify

  (-n|--name) <imageName>
        Specify the docker image name to use.

  [--skipDelete]
        Skip the deletion of docker container.

  [--createOutputDir]
        Create a specific directory for output.

  (-l|--logDirectory) <logDirectory>
        Specify where to put logs and serialized files created by docker
        machines.

  [(-t|--threads) <threads>]
        Specify the number of threads to run in parallel (default: 2)

  [(-g|--globalTimeout) <globalTimeout>]
        Specify the number of day before killing the whole pool. (default: 1)

  [--pushurl <pushUrl>]
        Specify repository URL to push data.

Please note that the GITHUB_OAUTH environment variables must be set.
```

### Realtime Scanner

This module located at [repairnator-realtime](/repairnator/repairnator-realtime) is used to scan in live the new jobs on TravisCI and to try repairing failing ones by using the [pipeline](#pipeline).

```bash
Usage: java <repairnator-realtime name> [option(s)]

Options: 

  [-h|--help]

  [-d|--debug]

  [--runId <runId>]
        Specify the run id for this launch.

  (-o|--output) <output>
        Specify where to put serialized files from dockerpool

  [--dbhost <mongoDBHost>]
        Specify mongodb host.

  [--dbname <mongoDBName>]
        Specify mongodb DB name. (default: repairnator)

  [--notifyEndProcess]
        Activate the notification when the process ends.

  [--smtpServer <smtpServer>]
        Specify SMTP server to use for Email notification

  [--notifyto notifyto1,notifyto2,...,notifytoN ]
        Specify email addresses to notify

  (-n|--name) <imageName>
        Specify the docker image name to use.

  [--skipDelete]
        Skip the deletion of docker container.

  [--createOutputDir]
        Create a specific directory for output.

  (-l|--logDirectory) <logDirectory>
        Specify where to put logs and serialized files created by docker
        machines.

  [(-t|--threads) <threads>]
        Specify the number of threads to run in parallel (default: 2)

  [--pushurl <pushUrl>]
        Specify repository URL to push data.

  [(-w|--whitelist) <whitelist>]
        Specify the path of whitelisted repository

  [(-b|--blacklist) <blacklist>]
        Specify the path of blacklisted repository

  [--jobsleeptime <jobsleeptime>]
        Specify the sleep time between two requests to Travis Job endpoint (in
        seconds) (default: 60)

  [--buildsleeptime <buildsleeptime>]
        Specify the sleep time between two refresh of build statuses (in
        seconds) (default: 10)

  [--maxinspectedbuilds <maxinspectedbuilds>]
        Specify the maximum number of watched builds (default: 100)

  [--duration <duration>]
        Duration of the execution. If not given, the execution never stop. This
        argument should be given on the ISO-8601 duration format: PWdTXhYmZs
        where W, X, Y, Z respectively represents number of Days, Hours, Minutes
        and Seconds. T is mandatory before the number of hours and P is always
        mandatory.

Please note that the GITHUB_OAUTH environment variables must be set.
```

### Scanner

This module located at [repairnator-scanner](/repairnator/repairnator-scanner) is used to detect failing builds from TravisCI over a period of time.
It produces a list of build ids (integers).

```bash
Usage: java <repairnator-scanner name> [option(s)]

Options: 

  [-h|--help]

  [-d|--debug]

  [--runId <runId>]
        Specify the run id for this launch.

  [--bears]
        This mode allows to use repairnator to analyze pairs of bugs and
        human-produced patches.

  (-i|--input) <input>
        Specify where to find the list of projects to scan.

  [(-o|--output) <output>]
        Specify where to write the list of build ids (default: stdout)

  [--dbhost <mongoDBHost>]
        Specify mongodb host.

  [--dbname <mongoDBName>]
        Specify mongodb DB name. (default: repairnator)

  [--notifyEndProcess]
        Activate the notification when the process ends.

  [--smtpServer <smtpServer>]
        Specify SMTP server to use for Email notification

  [--notifyto notifyto1,notifyto2,...,notifytoN ]
        Specify email addresses to notify

  [(-l|--lookupHours) <lookupHours>]
        Specify the hour number to lookup to get builds (default: 4)

  [(-f|--lookFromDate) <lookFromDate>]
        Specify the initial date to get builds (e.g. 01/01/2017). Note that the
        search starts from 00:00:00 of the specified date.

  [(-t|--lookToDate) <lookToDate>]
        Specify the final date to get builds (e.g. 31/01/2017). Note that the
        search is until 23:59:59 of the specified date.
        
  [--bearsMode <bearsMode>]
          This option is only useful in case of '--bears' is used: it defines the
          type of fixer build to get. Available values:
          failing_passing;passing_passing;both (default: both)

Please note that the GITHUB_OAUTH environment variables must be set.
``` 


### Checkbranches

This module located at [repairnator-checkbranches](/repairnator/repairnator-checkbranches) can be used to check the status of a reproduced bug pushed by the [pipeline](#pipeline) on a git specific branch. 

```bash
Usage: java <repairnator-checkbranches name> [option(s)]

Options: 

  [-h|--help]

  [-d|--debug]

  [--runId <runId>]
        Specify the run id for this launch.

  (-i|--input) <input>
        Specify the input file containing the list of branches to reproduce

  (-o|--output) <output>
        Specify where to put output data

  [--notifyEndProcess]
        Activate the notification when the process ends.

  [--smtpServer <smtpServer>]
        Specify SMTP server to use for Email notification

  [--notifyto notifyto1,notifyto2,...,notifytoN ]
        Specify email addresses to notify

  (-n|--name) <imageName>
        Specify the docker image name to use.

  [--skipDelete]
        Skip the deletion of docker container.

  [(-t|--threads) <threads>]
        Specify the number of threads to run in parallel (default: 2)

  [(-g|--globalTimeout) <globalTimeout>]
        Specify the number of day before killing the whole pool. (default: 1)

  [-p|--humanPatch]

  (-r|--repository) <repository>
        Specify where to collect branches

Please note that the GITHUB_OAUTH environment variables must be set.
```
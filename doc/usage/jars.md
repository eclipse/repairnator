# Jars usage

This page describe the usage of all modules of Repairnator. All modules are located under [repairnator directory](/repairnator).
The usage can all be find by executing the jars with dependencies with `-h` argument: 

```
$ cd repairnator-pipeline
$ java -jar target/repairnator-pipeline-2.3-SNAPSHOT-jar-with-dependencies.jar -h
```

In the following usages, optional arguments are marked with `[]` and mandatory ones are marked with `()`.

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

  --ghOauth <ghOauth>
        Specify Github Token to use
        
  (-b|--build) <build>
        Specify the build id to use.
        
  --repairTools repairTools1,repairTools2,...,repairToolsN 
        Specify one or several repair tools to use among:
        NopolAllTests,NPEFix,AssertFixer,AstorJGenProg,AstorJKali,NopolSingleTest,AstorJMut,NopolTestExclusionStrategy
        (default:
        NopolAllTests,NPEFix,AssertFixer,AstorJGenProg,AstorJKali,NopolSingleTest,AstorJMut,NopolTestExclusionStrategy)

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
        Specify repository URL to push data on the format
        https://github.com/user/repo.

  [--githubUserName <githubUserName>]
        Specify the name of the user who commits (default: repairnator)

  [--githubUserEmail <githubUserEmail>]
        Specify the email of the user who commits (default: noreply@github.com)

  [--createPR]
        Activate the creation of a Pull Request in case of patch.

  [(-n|--nextBuild) <nextBuild>]
        Specify the next build id to use (only in BEARS mode). (default: -1)

  [--z3 <z3>]
        Specify path to Z3 (default: ./z3_for_linux)

  [(-w|--workspace) <workspace>]
        Specify a path to be used by the pipeline at processing things like to
        clone the project of the build id being processed (default: ./workspace)

  [--projectsToIgnore <projectsToIgnore>]
        Specify the file containing a list of projects that the pipeline should
        deactivate serialization when processing builds from. (default:
        ./projects_to_ignore.txt)

The environment variable M2_HOME should be set and refer to the path of your maven home installation.
For using Nopol, you must add tools.jar in your classpath from your installed jdk
``` 

### Dockerpool

```bash
Usage: java <repairnator-dockerpool name> [option(s)]

Options: 

  --ghOauth <ghOauth>
        Specify Github Token to use
        
  (-n|--name) <imageName>
        Specify the docker image name to use.
        
  --repairTools repairTools1,repairTools2,...,repairToolsN 
        Specify one or several repair tools to use separated by commas
        (available tools might depend of your docker image)
        
  (-i|--input) <input>
        Specify the input file containing the list of build ids.

  (-o|--output) <output>
        Specify where to put serialized files from dockerpool
        
  (-l|--logDirectory) <logDirectory>
        Specify where to put logs and serialized files created by docker
        machines.

  [-h|--help]

  [-d|--debug]

  [--runId <runId>]
        Specify the run id for this launch.

  [--bears]
        This mode allows to use repairnator to analyze pairs of bugs and
        human-produced patches.

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

  [--skipDelete]
        Skip the deletion of docker container.

  [--createOutputDir]
        Create a specific directory for output.

  [(-t|--threads) <threads>]
        Specify the number of threads to run in parallel (default: 2)

  [(-g|--globalTimeout) <globalTimeout>]
        Specify the number of day before killing the whole pool. (default: 1)

  [--pushurl <pushUrl>]
        Specify repository URL to push data on the format
        https://github.com/user/repo.

  [--githubUserName <githubUserName>]
        Specify the name of the user who commits (default: repairnator)

  [--githubUserEmail <githubUserEmail>]
        Specify the email of the user who commits (default: noreply@github.com)

  [--createPR]
        Activate the creation of a Pull Request in case of patch.
```

### Realtime Scanner

```bash
Usage: java <repairnator-realtime name> [option(s)]

Options: 

  --ghOauth <ghOauth>
        Specify Github Token to use

  --repairTools <repairTools>
        Specify one or several repair tools to use separated by commas
        (available tools might depend of your docker image)
        
  (-o|--output) <output>
        Specify where to put serialized files from dockerpool
        
  (-n|--name) <imageName>
        Specify the docker image name to use.
        
  (-l|--logDirectory) <logDirectory>
        Specify where to put logs and serialized files created by docker
        machines.

  [-h|--help]

  [-d|--debug]

  [--runId <runId>]
        Specify the run id for this launch.

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

  [--skipDelete]
        Skip the deletion of docker container.

  [--createOutputDir]
        Create a specific directory for output.

  [(-t|--threads) <threads>]
        Specify the number of threads to run in parallel (default: 2)

  [--pushurl <pushUrl>]
        Specify repository URL to push data on the format
        https://github.com/user/repo.

  [--githubUserName <githubUserName>]
        Specify the name of the user who commits (default: repairnator)

  [--githubUserEmail <githubUserEmail>]
        Specify the email of the user who commits (default: noreply@github.com)

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
        
  [--createPR]
          Activate the creation of a Pull Request in case of patch.
```

### Scanner

```bash
Usage: java <repairnator-scanner name> [option(s)]

Options: 

  --ghOauth <ghOauth>
        Specify Github Token to use

  (-i|--input) <input>
        Specify where to find the list of projects to scan.

  [-h|--help]

  [-d|--debug]

  [--runId <runId>]
        Specify the run id for this launch.

  [--bears]
        This mode allows to use repairnator to analyze pairs of bugs and
        human-produced patches.

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

  [--bearsDelimiter]
        This option is only useful in case of '--bears' is used and '--bearsMode
        both' (default) is used: it allows to define a delimiter to output the
        failing passing and then the passing passing in order to consider them
        separately

``` 


### Checkbranches

```bash
Usage: java <repairnator-checkbranches name> [option(s)]

Options: 

  (-r|--repository) <repository>
        Specify where to collect branches
        
  (-n|--name) <imageName>
        Specify the docker image name to use.
        
  (-i|--input) <input>
        Specify the input file containing the list of branches to reproduce

  (-o|--output) <output>
        Specify where to put output data

  [-h|--help]

  [-d|--debug]

  [--runId <runId>]
        Specify the run id for this launch.

  [--bears]
        This mode allows to use repairnator to analyze pairs of bugs and
        human-produced patches.

  [--notifyEndProcess]
        Activate the notification when the process ends.

  [--smtpServer <smtpServer>]
        Specify SMTP server to use for Email notification

  [--notifyto notifyto1,notifyto2,...,notifytoN ]
        Specify email addresses to notify

  [--skipDelete]
        Skip the deletion of docker container.

  [(-t|--threads) <threads>]
        Specify the number of threads to run in parallel (default: 2)

  [(-g|--globalTimeout) <globalTimeout>]
        Specify the number of day before killing the whole pool. (default: 1)

  [-p|--humanPatch]

```
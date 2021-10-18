# Repairnator Command Line Usage

This page describes the usage of all main classes of Repairnator.

## Pipeline fr.inria.spirals.repairnator.pipeline.Launcher

### Travis CI launcher

By default, it analyzes Travis CI builds. This is equivalent to using the option `--launcherMode REPAIR`

The following command line tools must be installed on your machine:
  - Oracle Java: some dependencies need tools.jar in the classpath;
  - cloc: we compute some metrics with cloc;
  - git: to apply some git commands;
  - z3: a constraint solver (the executables are available in [pipeline test resources](/src/repairnator-pipeline/src/test/resources/z3)).

```
$ git clone https://github.com/eclipse/repairnator/
$ cd repairnator/
$ mvn clean install -DskipTests -f src/repairnator-core/ && mvn clean install -DskipTests -f src/repairnator-pipeline/
$ cd src/repairnator-pipeline/
```

Run it on Travis CI build [224246334](https://travis-ci.com/github/repairnator/failingProject/builds/224246334)

```
export M2_HOME=/usr/share/maven
export GITHUB_TOKEN=foobar # your Token
export TOOLS_JAR=/usr/lib/jvm/default-java/lib/tools.jar

java -cp $TOOLS_JAR:target/repairnator-pipeline-3.3-SNAPSHOT-jar-with-dependencies.jar fr.inria.spirals.repairnator.pipeline.Launcher --ghOauth $GITHUB_TOKEN -b 224246334
```

Options

```bash
Usage: java <repairnator-pipeline name> [option(s)]

Options:

  --ghOauth <ghOauth>
        Specify GitHub Token to use.

  (-b|--build) <build>
        Specify the Travis build ID to use.

  --repairTools repairTools1,repairTools2,...,repairToolsN
        Specify one or several repair tools to use among:
        NopolAllTests,NPEFix,AssertFixer,AstorJGenProg,AstorJKali,NopolSingleTest,AstorJMut,NopolTestExclusionStrategy
        (default:
        NopolAllTests,NPEFix,AssertFixer,AstorJGenProg,AstorJKali,NopolSingleTest,AstorJMut,NopolTestExclusionStrategy).

  [-h|--help]

  [-d|--debug]

  [--runId <runId>]
        Specify the run ID for this launch.

  [--bears]
        This mode allows to use Repairnator to analyze pairs of bugs and
        human-produced patches.

  [(-o|--output) <output>]
        Specify the path to output serialized files.

  [--dbhost <mongoDBHost>]
        Specify MongDB host.

  [--dbname <mongoDBName>]
        Specify MongoDB DB name (default: repairnator).

  [--smtpServer <smtpServer>]
        Specify SMTP server to use for Email notification.

  [--notifyto notifyto1,notifyto2,...,notifytoN ]
        Specify email addresses to notify.

  [--pushurl <pushUrl>]
        Specify repository URL to push data on the format
        https://github.com/user/repo.

  [--githubUserName <githubUserName>]
        Specify the name of the user who commits (default: repairnator).

  [--githubUserEmail <githubUserEmail>]
        Specify the email of the user who commits (default: noreply@github.com).

  [--createPR]
        Activate the creation of a Pull Request in case of patch.

  [(-n|--nextBuild) <nextBuild>]
        Specify the next build ID to use (only in BEARS mode) - (default: -1).

  [--z3 <z3>]
        Specify path to Z3 (default: ./z3_for_linux).

  [(-w|--workspace) <workspace>]
        Specify a path to be used by the pipeline at processing things like to
        clone the project of the build ID being processed (default: ./workspace).

  [--projectsToIgnore <projectsToIgnore>]
        Specify the file containing a list of projects that the pipeline should
        deactivate serialization when processing builds from (default:
        ./projects_to_ignore.txt).

The environment variable M2_HOME should be set and refer to the path of your Maven home installation.
To use Nopol, you must add tools.jar in your classpath from your installed JDK.
```

### GitHub launcher

It is also possible to run it on a GitHub repository that contains a Java Maven project, e.g., this one: https://github.com/repairnator/failingProject.

For that, we need to specify the launcher mode with the option `--launcherMode GIT_REPOSITORY`, and the parameter `--gitrepourl` for specifying a Git repository URL.
The URL has the following format: `https://github.com/user/repo` (without the final `.git`).

```
export M2_HOME=/usr/share/maven
export GITHUB_TOKEN=foobar # your Token
export TOOLS_JAR=/usr/lib/jvm/default-java/lib/tools.jar

java -cp $TOOLS_JAR:target/repairnator-pipeline-3.3-SNAPSHOT-jar-with-dependencies.jar fr.inria.spirals.repairnator.pipeline.Launcher --ghOauth $GITHUB_TOKEN --launcherMode GIT_REPOSITORY --gitrepourl https://github.com/repairnator/failingProject
```

In addition to the Launcher options, the `GIT_REPOSITORY` launcher mode offers other options, which are the following:

Options

```bash
Usage: java <repairnator-pipeline name> [option(s)]

Options:

  [--gitrepourl <gitRepositoryUrl>]
        Specify a Git repository URL (only in GIT_REPOSITORY mode).

  [--gitrepobranch <gitRepositoryBranch>]
        Specify a branch of the given repository (only in GIT_REPOSITORY mode) - (default: master branch).

  [--gitrepoidcommit <gitRepositoryIdCommit>]
        Specify the commit id of the given repository (only in GIT_REPOSITORY mode).

  [--gitrepofirstcommit]
        Decides whether to clone the first commit of the specified branch (only in GIT_REPOSITORY mode).

```

### gitrepofirstcommit

When Repairnator is executed in `GIT_REPOSITORY` launcher mode, it clones the repository associated with the parameter `gitrepourl`, and it is executed on the latest commit pushed on master branch.

Using the parameter `gitrepofirstcommit`, on the contrary, Repairnator will be executed on the first commit (the oldest one) of the specified repository.

Example of use:

```
export M2_HOME=/usr/share/maven
export GITHUB_TOKEN=foobar # your Token
export TOOLS_JAR=/usr/lib/jvm/default-java/lib/tools.jar

java -cp $TOOLS_JAR:target/repairnator-pipeline-3.3-SNAPSHOT-jar-with-dependencies.jar fr.inria.spirals.repairnator.pipeline.Launcher --ghOauth $GITHUB_TOKEN --launcherMode GIT_REPOSITORY --gitrepourl <git-repository-url> --gitrepofirstcommit
```

### gitrepoidcommit

Using the parameter ``gitrepoidcommit`` followed by the commit ID, Repairnator is executed on that specific commit associated with the repository specified with the parameter `gitrepourl`.

Example of use:

```
export M2_HOME=/usr/share/maven
export GITHUB_TOKEN=foobar # your Token
export TOOLS_JAR=/usr/lib/jvm/default-java/lib/tools.jar

java -cp $TOOLS_JAR:target/repairnator-pipeline-3.3-SNAPSHOT-jar-with-dependencies.jar fr.inria.spirals.repairnator.pipeline.Launcher --ghOauth $GITHUB_TOKEN --launcherMode GIT_REPOSITORY --gitrepourl <git-repository-url> --gitrepoidcommit <git-sha>
```

### gitrepobranch

It is also possible to run Repairnator directly on a specific branch (in this case, Repairnator clones only the specified branch of the Git repository) using the parameter `gitrepobranch` followed by the name of the branch. When a branch name is provided, it is also possible to use the parameters `gitrepoidcommit` or `gitrepofirstcommit` to run Repairnator on a specific commit of the provided branch.

Example of use:

```
export M2_HOME=/usr/share/maven
export GITHUB_TOKEN=foobar # your Token
export TOOLS_JAR=/usr/lib/jvm/default-java/lib/tools.jar

java -cp $TOOLS_JAR:target/repairnator-pipeline-3.3-SNAPSHOT-jar-with-dependencies.jar fr.inria.spirals.repairnator.pipeline.Launcher --ghOauth $GITHUB_TOKEN --launcherMode GIT_REPOSITORY --gitrepourl <git-repository-url> --gitrepobranch <branch-name>
```

### Fault localization mode

Repairnator also features a fault localization mode. 
In this mode, a git repository is analyzed, and the identified suspicious lines are pushed as a PR review/suggestions to the original PR.

The example below shows the usage of this mode:

```bash
export M2_HOME=/usr/share/maven
export GITHUB_TOKEN=foobar # your token

java -cp target/repairnator-pipeline-3.3-SNAPSHOT-jar-with-dependencies.jar fr.inria.spirals.repairnator.pipeline.Launcher --launcherMode FAULT_LOCALIZATION --faultLocalization --gitrepourl "https://github.com/andre15silva/failingProject" --gitrepoidcommit 54f241129e09d71955b7d2f4fc7f496118b3e1c6 --flacocoThreshold 0.12 --ghOauth $GITHUB_TOKEN
```

The result of running this example can be found [here](https://github.com/repairnator/failingProject/pull/7).

## Realtime Scanner fr.inria.spirals.repairnator.realtime.RTLauncher

```bash
Usage: java <repairnator-realtime name> [option(s)]

Options:

  --ghOauth <ghOauth>
        Specify GitHub Token to use.

  --repairTools <repairTools>
        Specify one or several repair tools to use separated by commas
        (available tools might depend of your Docker image).

  (-o|--output) <output>
        Specify where to put serialized files from dockerpool.

  (-n|--name) <imageName>
        Specify the Docker image name to use.

  (-l|--logDirectory) <logDirectory>
        Specify where to put logs and serialized files created by Docker
        machines.

  [-h|--help]

  [-d|--debug]

  [--runId <runId>]
        Specify the run ID for this launch.

  [--dbhost <mongoDBHost>]
        Specify MongoDB host.

  [--dbname <mongoDBName>]
        Specify MongoDB DB name (default: repairnator).

  [--notifyEndProcess]
        Activate the notification when the process ends.

  [--smtpServer <smtpServer>]
        Specify SMTP server to use for Email notification.

  [--notifyto notifyto1,notifyto2,...,notifytoN ]
        Specify email addresses to notify.

  [--skipDelete]
        Skip the deletion of Docker container.

  [--createOutputDir]
        Create a specific directory for output.

  [(-t|--threads) <threads>]
        Specify the number of threads to run in parallel (default: 2).

  [--pushurl <pushUrl>]
        Specify repository URL to push data on the format
        https://github.com/user/repo.

  [--githubUserName <githubUserName>]
        Specify the name of the user who commits (default: repairnator).

  [--githubUserEmail <githubUserEmail>]
        Specify the email of the user who commits (default: noreply@github.com).

  [(-w|--whitelist) <whitelist>]
        Specify the path of whitelisted repository.

  [(-b|--blacklist) <blacklist>]
        Specify the path of blacklisted repository.

  [--jobsleeptime <jobsleeptime>]
        Specify the sleep time between two requests to Travis Job endpoint (in
        seconds) - (default: 60).

  [--buildsleeptime <buildsleeptime>]
        Specify the sleep time between two refresh of build statuses (in
        seconds) - (default: 10).

  [--maxinspectedbuilds <maxinspectedbuilds>]
        Specify the maximum number of watched builds (default: 100).

  [--duration <duration>]
        Duration of the execution. If it is not given, the execution never stops. This
        argument should be given on the ISO-8601 duration format: PWdTXhYmZs
        where W, X, Y, Z respectively represents number of Days, Hours, Minutes
        and Seconds. T is mandatory before the number of hours and P is always
        mandatory.

  [--createPR]
          Activate the creation of a Pull Request in case of patch.
```


### Checkbranches

```bash
Usage: java <repairnator-checkbranches name> [option(s)]

Options:

  (-r|--repository) <repository>
        Specify where to collect branches.

  (-n|--name) <imageName>
        Specify the Docker image name to use.

  (-i|--input) <input>
        Specify the input file containing the list of branches to reproduce.

  (-o|--output) <output>
        Specify where to put output data.

  [-h|--help]

  [-d|--debug]

  [--runId <runId>]
        Specify the run ID for this launch.

  [--bears]
        This mode allows to use Repairnator to analyze pairs of bugs and
        human-produced patches.

  [--notifyEndProcess]
        Activate the notification when the process ends.

  [--smtpServer <smtpServer>]
        Specify SMTP server to use for Email notification.

  [--notifyto notifyto1,notifyto2,...,notifytoN ]
        Specify email addresses to notify.

  [--skipDelete]
        Skip the deletion of Docker container.

  [(-t|--threads) <threads>]
        Specify the number of threads to run in parallel (default: 2).

  [(-g|--globalTimeout) <globalTimeout>]
        Specify the number of days before killing the whole pool (default: 1).

  [-p|--humanPatch]

```

## fr.inria.spirals.repairnator.dockerpool.BuildAnalyzerLauncher

(internal usage only)

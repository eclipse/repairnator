To run Repairnator directly on a Git repository, it is necessary to use the `GIT_REPOSITORY` launcher mode, using the parameter `gitrepo` and specifying a Git repository URL with the parameter `gitrepourl`. The URL has the following format: https://github.com/user/repo (without the final `.git`).

The `GIT_REPOSITORY` launcher mode offers different options and they are described below:

### gitrepofirstcommit

When Repairnator is executed in `GIT_REPOSITORY` launcher mode, it clones the repository associated with the parameter `gitrepourl`, and it is executed on the latest commit pushed on master branch.

Using the parameter `gitrepofirstcommit`, on the contrary, Repairnator will be executed on the first commit (the oldest one) of the specificied repository.

Example of use:

```
export M2_HOME=/usr/share/maven
export GITHUB_TOKEN=foobar # your Token
export TOOLS_JAR=/usr/lib/jvm/default-java/lib/tools.jar

java -cp $TOOLS_JAR:target/repairnator-pipeline*.jar fr.inria.spirals.repairnator.pipeline.Launcher --ghOauth $GITHUB_TOKEN --gitrepo --gitrepourl <git-repository-url> --gitrepofirstcommit
```

### gitrepoidcommit

Using the parameter ``gitrepoidcommit`` followed by the commit ID, Repairnator is executed on that specific commit associated with the repository specified with the parameter `gitrepourl`.

Example of use:

```
export M2_HOME=/usr/share/maven
export GITHUB_TOKEN=foobar # your Token
export TOOLS_JAR=/usr/lib/jvm/default-java/lib/tools.jar

java -cp $TOOLS_JAR:target/repairnator-pipeline*.jar fr.inria.spirals.repairnator.pipeline.Launcher --ghOauth $GITHUB_TOKEN --gitrepo --gitrepourl <git-repository-url> --gitrepoidcommit <git-sha>
```

### gitrepobranch

It is also possible to run Repairnator directly on a specific branch (in this case, Repairnator clones only the specified branch of the Git repository) using the parameter `gitrepobranch` followd by the name of the branch. When a branch name is provided, it is also possible to use the parameters `gitrepoidcommit` or `gitrepofirstcommit` to run Repairnator on a specific commit of the provided branch.

Example of use:

```
export M2_HOME=/usr/share/maven
export GITHUB_TOKEN=foobar # your Token
export TOOLS_JAR=/usr/lib/jvm/default-java/lib/tools.jar

java -cp $TOOLS_JAR:target/repairnator-pipeline*.jar fr.inria.spirals.repairnator.pipeline.Launcher --ghOauth $GITHUB_TOKEN --gitrepo --gitrepourl <git-repository-url> --gitrepobranch <branch-name>
```

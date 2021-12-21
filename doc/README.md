# Documentation of Repairnator
# Documentation of Repairnator

This directory provides the official documentation about the Repairnator project.

## Overview

Repairnator is a bot to automatically repair failing continuous integration builds. By repairing we mean synthesizing and proposing patches to the developer.

Repairnator can be used:

* [as a Jenkins plugin](https://github.com/eclipse/repairnator/blob/master/doc/repairnator-jenkins-plugin.md)
* [as a Github app](https://github.com/eclipse/repairnator/blob/master/doc/repairnator-github-app.md)
* as a command line tool, see "Command line" below
* as a Travis CI scanner, see "Travis Scanner" below
* as Pull Requests scanner with Flacocobot, see "Pull Requests (Flacoco) Scanner" below

## Contributing

Contributions to Repairnator are more than welcome!
A first way to contribute is to look at the label [good-first-issue](https://github.com/eclipse/repairnator/labels/good-first-issue).

## Running Repairnator

### Command line

To fix a specific Travis CI build:

```
docker run -e BUILD_ID=235838150 -e GITHUB_OAUTH=<GITHUB TOKEN> repairnator/pipeline
```

It is possible to create your own GitHub token from [this page](https://github.com/settings/tokens), by selecting `public_repo` referring to the scope that has to be associated with the token.

You can also specify the values associated with these parameters: [REPAIR_TOOLS](https://github.com/eclipse/repairnator/blob/a78745d1f6c0cf4d356cfc20485030fc0f18eb79/doc/repairnator-config.md#repair_tools), [MONGODB_HOST](https://github.com/eclipse/repairnator/blob/a78745d1f6c0cf4d356cfc20485030fc0f18eb79/doc/repairnator-config.md#mongodb_host), [MONGODB_NAME](https://github.com/eclipse/repairnator/blob/a78745d1f6c0cf4d356cfc20485030fc0f18eb79/doc/repairnator-config.md#mongodb_name), [PUSH_URL](https://github.com/eclipse/repairnator/blob/a78745d1f6c0cf4d356cfc20485030fc0f18eb79/doc/repairnator-config.md#push_url), [SMTP_SERVER](https://github.com/eclipse/repairnator/blob/a78745d1f6c0cf4d356cfc20485030fc0f18eb79/doc/repairnator-config.md#smtp_server), [NOTIFY_TO](https://github.com/eclipse/repairnator/blob/a78745d1f6c0cf4d356cfc20485030fc0f18eb79/doc/repairnator-config.md#notify_to), [GITHUB_USERNAME](https://github.com/eclipse/repairnator/blob/a78745d1f6c0cf4d356cfc20485030fc0f18eb79/doc/repairnator-config.md#github_username), [GITHUB_USEREMAIL](https://github.com/eclipse/repairnator/blob/a78745d1f6c0cf4d356cfc20485030fc0f18eb79/doc/repairnator-config.md#github_useremail), and [CREATE_PR](https://github.com/eclipse/repairnator/blob/a78745d1f6c0cf4d356cfc20485030fc0f18eb79/doc/repairnator-config.md#create_pr). Before every parameter name, it is necessary to put the key `-e`, as in the above example.

### Travis Scanner

The scanner continuously pulls new build identifiers from Travis CI.

Prerequisites: Java, Docker

```
git clone https://github.com/eclipse/repairnator/
cd repairnator/src/scripts/
# doc at https://github.com/eclipse/repairnator/blob/master/doc/scripts.md#launch_rtscannersh
bash launch_rtscanner.sh
```

### Pull Requests (Flacoco) Scanner

Prerequisites: Java, Docker

```
git clone https://github.com/eclipse/repairnator/
cd repairnator/src/repairnator-realtime
mvn clean package -DskipTests
java -cp target/repairnator-realtime-<version>-jar-with-dependencies.jar fr.inria.spirals.repairnator.realtime.FlacocoScanner
# doc at https://github.com/eclipse/repairnator/blob/master/doc/main-classes.md#flacoco-scanner
```

## Program repair tools used in Repairnator
 
For more information about the program repair tools and their strategies implemented in Repairnator, [take a look at this page](repair-tools.md).

To add a new program repair tool in Repairnator, [here is the guide](add-repair-tool.md).

## Architecture

### The pipeline

The pipeline takes as input a Travis CI build ID and tries to replicate the bug and to repair it with different repair tools. It provides a single abstraction over repair tools. As its name indicates, it's a pipeline of steps, from cloning the repository and building it, to launching the repair tools and pushing a resulting branch. It contains plenty of options, for notifying the users, creating a pull request, etc.

The pipeline can be used:
* directly in Java
* or as a Docker image: it encapsulates the pipeline in an environment in which it will properly work, with the right dependencies, in a sandboxed environment.

### The scanner

The `RTScanner` is a daemon that constantly inspects Travis CI for catching newly failing builds to launch as fast as possible. When failing builds are found, a pipeline is run per build. `FastScanner` is a different implementation of the same idea.

[Deployment in Kubernetes](https://github.com/eclipse/repairnator/blob/master/doc/repairnator-kubernetes.md)

### Java main classes

This [page](main-classes.md) documents the main classes of Repairnator.

### Chores

As part of using Repairnator, you might need to do some chores, like managing a MongoDB database.
We provided some documentation [about backups](chore/managedb.md) and [about MongoDB collection schema](chore/mongo).

### Scripts

The documentation about the scripts is [available here](scripts.md). More details about the Repairnator configuration file can be found [here](repairnator-config.md).


### Automated Pull-requests

Repairnator can be configured to create pull-requests automatically.
To enable this functionality, see the `--createPR` option in the [Command Line Usage](https://github.com/eclipse/repairnator/blob/master/doc/main-classes.md) documentation.

The PR uses the text found on [this file](https://github.com/eclipse/repairnator/blob/master/src/repairnator-pipeline/src/main/resources/R-Hero-PR-text.MD). In the future, it is planned to generate a specific piece of text for each patch (see [Explainable Software Bot Contributions: Case Study of Automated Bug Fixes](http://arxiv.org/pdf/1905.02597) ([doi:10.1109/BotSE.2019.00010](https://doi.org/10.1109/BotSE.2019.00010)))

The PR text contains feedback links, meant to be handled by an instance of the [R-Hero feedback service](https://github.com/repairnator/r-hero-feedback-service).

## Content of the repository

This repository is organized as follows:

  * [doc](../doc) contains the reference documentation about Repairnator and its usage;
  * [repairnator](../repairnator) is the main program dedicated to this project: it can automatically scan a large set of projects, detect failing builds, reproduce them and try to repair them using our tools;
  * [resources](../resources) contains mainly data produced by Repairnator, scripts to retrieve those data, and it also contains the schema of repairnator.json files;
  * [website](../website) contains all data to produce repairnator website.

## Academic bibliographic references

"[A Software Repair Bot based on Continual Learning](http://arxiv.org/pdf/2012.06824)"  In IEEE Software, 2021. ([doi:10.1109/ms.2021.3070743](https://doi.org/10.1109/ms.2021.3070743))

```
@article{arXiv-2012.06824,
 title = {A Software Repair Bot based on Continual Learning},
 author = {Benoit Baudry and Zimin Chen and Khashayar Etemadi and Han Fu and Davide Ginelli and Steve Kommrusch and Matias Martinez and Martin Monperrus and Javier Ron and He Ye and Zhongxing Yu},
 journal = {IEEE Software},
 year = {2021},
 doi = {10.1109/ms.2021.3070743},
}
```


"[Repairnator patches programs automatically](https://ubiquity.acm.org/article.cfm?id=3349589)", In Ubiquity, Association for Computing Machinery, vol. July, no. 2, pp. 1-12, 2019. 

```
@article{monperrus:hal-02267512,
 title = {Repairnator patches programs automatically},
 author = {Monperrus, Martin and Urli, Simon and Durieux, Thomas and Martinez, Martin and Baudry, Benoit and Seinturier, Lionel},
 url = {https://hal.inria.fr/hal-02267512/file/repairnator.pdf},
 journal = {{Ubiquity}},
 publisher = {{Association for Computing Machinery}},
 volume = {July},
 number = {2},
 pages = {1-12},
 year = {2019},
 doi = {10.1145/3349589},
}
```

"[How to Design a Program Repair Bot? Insights from the Repairnator Project](https://hal.inria.fr/hal-01691496/file/SEIP_63_Camera-Ready-no-copyright.pdf)", In 40th International Conference on Software Engineering, Track Software Engineering in Practice, pp. 95-104, 2018. 

```
@inproceedings{urli:hal-01691496,
 title = {How to Design a Program Repair Bot? Insights from the Repairnator Project},
 author = {Urli, Simon and Yu, Zhongxing and Seinturier, Lionel and Monperrus, Martin},
 url = {https://hal.inria.fr/hal-01691496/file/SEIP_63_Camera-Ready-no-copyright.pdf},
 booktitle = {{40th International Conference on Software Engineering, Track Software Engineering in Practice}},
 pages = {95-104},
 year = {2018},
 doi = {10.1145/3183519.3183540},
}

```

"[Bears: An Extensible Java Bug Benchmark for Automatic Program Repair Studies](https://arxiv.org/pdf/1901.06024)", In SANER 2019 - 26th IEEE International Conference on Software Analysis, Evolution and Reengineering, 2019. 

```
@inproceedings{madeiral:hal-01990052,
 title = {Bears: An Extensible Java Bug Benchmark for Automatic Program Repair Studies},
 author = {Madeiral, Fernanda and Urli, Simon and Maia, Marcelo and Monperrus, Martin},
 url = {https://arxiv.org/pdf/1901.06024},
 booktitle = {{SANER 2019 - 26th IEEE International Conference on Software Analysis, Evolution and Reengineering}},
 year = {2019},
 doi = {10.1109/SANER.2019.8667991},
}
```

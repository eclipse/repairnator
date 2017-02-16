# RepairNator

## What it is?

The first goal of RepairNator is to scan failing Travis builds, try to reproduce the failing build and to repair it.
Different inputs can be given to RepairNator:
   * a list of Github Project name (like Spirals-Team/librepair)
   * a list of Travis Build IDs
   
## How to use it?

### Installation

Before packaging RepairNator you need to install the following dependencies: 
   * maven surefire-report-parser: a custom fork to add a feature (waiting for the PR to be accepted)
   * Nopol: an automatic repair software of the team
   
For Nopol, follow the instructions given [there](https://github.com/SpoonLabs/nopol#getting-started) until step 3.

For maven surefire-report-parser, just clone [this fork](https://github.com/surli/maven-surefire) and execute:

```
$ git checkout surefire-parser-feature-error-status
$ cd surefire-report-parser
$ mvn install -DskipTests=true
```

### Usage

RepairNator take a several number of arguments. We will describe them here.

Options : 

  - [-h|--help] Display the usage

  - [-d|--debug] Show debug informations from RepairNator and JTravis

  - [-p|--push] If set this flag push repository content (limited to 10 commits) in case of reproduced failing builds

  - [--clean] Clean workspace after each finished process.

  - (-i|--input) <input> Specify where to find the list of projects or builds to scan.

  - (-m|--mode) <mode> Specify if the input contains project names (SLUG), build ids (BUILD),
        path to repair (NOPOLONLY), or if it is to inspect passing builds
        (FORBEARS).

  - (-o|--output) <output> Specify where to place JSON/CSV output.

  - (-w|--workspace) <workspace> Specify where to clone failing repository (default: ./workspace)

  - (-l|--lookup) <lookup> Specify the number of hours to lookup in past for builds (default: 1)

  - [(-z|--z3Path) <z3Path>] Specify the solver path used by Nopol (mandatory in all modes except FORBEARS)

  - [(-g|--googleSecretPath) <googleSecretPath>] Specify the path of the google client secret file (default:
        ./client_secret.json)
        
Note that some environment variable must also be set:

  - M2_HOME: path to maven installation
  - GITHUB_LOGIN: login github
  - GITHUB_OAUTH: token github (those information are used to push but also to get information from GitHub API)

## Why "RepairNator"?
Because if the main objective of Terminator was "Seek and Destroy", the main goal of RepairNator is "Scan and Repair".
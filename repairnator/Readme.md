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
$ cd surefire-report-parser
$ mvn install -DskipTests=true
```

### Usage

RepairNator take a several number of arguments. We will describe them here.

## Why "RepairNator"?
Because if the main objective of Terminator was "Seek and Destroy", the main goal of RepairNator is "Scan and Repair".
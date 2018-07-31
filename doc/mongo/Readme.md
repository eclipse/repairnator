# Repairnator MongoDB specification

This document provides information about the data saved in MongoDB by Repairnator.

## Collections

### Repairnator

We currently store data in several collections: 
   - blacklisted: store data about blacklisted projects when using the realtime scanner
   - endprocess: store information about the status when a process ends. Note that it does not work with the realtime scanner.
   - hardwareInfo: provides some data on the machine on which a build is executed.
   - inspector: provides all useful information about the status of a build
   - metrics: gives some metrics about the build
   - patches: record the computed patches
   - pipeline-errors: store data about the errors encountered when trying to repair a build
   - rtscanner: gather data about the scanned builds (only for RTScanner)
   - scanner: gather data about the scanned projects (only for Scanner)
   - times: provides information about the duration of different pipeline steps
   - tool-diagnostic: provides advanced information about the different repair tool results
   - treatedbuild: record data about the dockerpool activity
   
### Bears

Some collections are specifically used for the bears project:
   - bearsmetrics: is a collection to provides more metrics on a project
   - inspector4bears: provides all useful information about the status of a build 
   - times4bears: provides information about the duration of different pipeline steps

### Deprecated collections

Other collections were also used in the past and are also described here:
  - astor: was used to store data about Astor repair tool usage
  - nopol: same as for astor but for Nopol repair tool
  - npefix: idem for NPEFix
  - withoutBranchesBugs: this collection contains some old records from inspectors that have been deleted because the branch were not recorded on Github.
# Repairnator MongoDB specification

This document provides information about the data saved in MongoDB by Repairnator.

## Collections

### Repairnator

We currently store data in several collections: 
   - [blacklisted](blacklisted-schema.json): store data about blacklisted projects when using the realtime scanner
   - [endprocess](endprocess-schema.json): store information about the status when a process ends. Note that it does not work with the realtime scanner.
   - [hardwareInfo](hardwareInfo-schema.json): provides some data on the machine on which a build is executed.
   - [inspector](inspector-schema.json): provides all useful information about the status of a build
   - [metrics](metrics-schema.json): gives some metrics about the build
   - [patches](patches-schema.json): record the computed patches
   - [pipeline-errors](pipeline-errors-schema.json): store data about the errors encountered when trying to repair a build
   - [rtscanner](rtscanner-schema.json): gather data about the scanned builds (only for RTScanner)
   - [scanner](scanner-schema.json): gather data about the scanned projects (only for Scanner)
   - [times](times-schema.json): provides information about the duration of different pipeline steps
   - [tool-diagnostic](tool-diagnostic-schema.json): provides advanced information about the different repair tool results
   - [treatedbuild](treatedbuild-schema.json): record data about the dockerpool activity
   
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
  
## How to link data between the different collections?

Three kind of IDs are used in the collections: 
  - MongoDB UUID: it's an automatic ID different for each collection, so it cannot be used to link datas;
  - Build ID: it's the Travis ID of a specific build. It can be used to link data, however one's has to be careful: the same Build ID might have been computed at different times;
  - Run ID: this ID, used with Build ID, guarantees the unicity of the computation of a build ID. So they should be used together to link the datas.
  
    
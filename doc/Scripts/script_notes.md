# Scripts
## launch_repairnator.sh
The launch repairnator has two options:
```
./launch_repairnator.sh
```
starts an instance of repairnator that takes the projects listed in ```~repairnator/project_list.txt``` //Not necessarily true, specify as in .cfg
and scans for builds made in the timespan specified in the config
file. If it finds builds it will start a dockerpool and attempt to
repair each build by starting a docker-pool which in turn starts a
repairnator pipeline on each build, which attempts to repair the
build.

```
./launch_repairnator.sh list_of_buildIDs.txt
```
Instead of scanning the builds in ```project_list.txt``` as done with
no arguments, this will instead attemt a repair on each of the builds
specified in ```list_of_buildIDs.txt``` in the same way as above.


## repair_buggy_build.sh
```
./repair_buggy_build.sh BUILDID
```
starts a single instance of repairnator-pipeline which attempts to
repair the build specified by the BUILDID.

## launch_rtscanner.sh
```
./launch_rtscanner.sh
```
starts a scanner which in realtime finds builds with failing tests
which it then attempts to repair. Is run for as long as specified by
the DURATION option in the config file.

## launch

# Scripts
Logs from running all of these scripts can be found in the
`HOME_REPAIR` directory as specified in the `repairnator.cfg` file.

## launch_repairnator.sh
The launch repairnator script can be used via the command:
```
./launch_repairnator.sh [list_ofBuildIDs.txt]
```
If the script is ran with the optional argument, it will start an
instance of repairnator-pipeline for each build specified in the
`list_of_buildIDs.txt`
and attempt to repair them by starting a
dockerpool and launching a docker container with an instance of
repairnator-pipeline for each of the specified buildIDs.

Without the optional argument, the script will instead search for the
file `project_list.txt` placed in the `HOME_REPAIR` directory as
specified in the local repairnator.cfg file. Each project should be of
the form `surli/failingProject`, that is everything following
`github.com/` when looking at a specific project.

Once this list has been read, repairnator will run a scanner to find
builds to repair. The timespan in which the scanner will look is
specified by either `SCANNER_NB_HOURS` in the local
repairnator.cfg, or by the two options `LOOK_FROM_DATE` and
`LOOK_TO_DATE` together. Once it has found builds to repair, it
will attempt to repair each of them by starting a dockerpool with a
docker container running an instance of repairnator-pipeline for each
of the found builds.

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
Starts a scanner which looks for failing builds in realtime. For each
it finds it will start a docker container with an instance of
repairnator-pipeline that tries to repair the build. It will run for
as long as specified by the DURATION option in the repairnator.cfg file.

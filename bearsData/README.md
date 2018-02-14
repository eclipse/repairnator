# Bears data

`list_of_projects` folder contains lists of repositoriy names hosted on GitHub, which are the projects used to obtain bugs from.

`scanned_builds` folder contains lists of scanned builds from projects in the folder `list_of_projects` during the period of January-May 2017. These scanned builds are the builds to be reprocuded in order to confirm whether they are buggy builds and the immediately builds next to them are fixer builds.

`branches` folder contains information on the branches generated in [Bears collection](https://github.com/surli/bugs-collection) by reprocuding scanned builds in the folder `scanned_builds`:

  * `1_reproduced_build_branches` folder contains lists of branches generated when buggy builds and their fixer builds were successfully reproduced.
  * `2_validation_results` folder contains lists of branches (containing in `1_reproduced_build_branches` folder) reproduced by a second time in order to validate them. In these lists, the branches are annotated with the status of the validation, where [OK] indicates successfully validated branches and [FAILURE] indicates unsuccessfully ones.
  * `3_successfully_validated_branches` folder contains lists of branches that were successfully validated, i.e. with [OK] status in the folder `2_validation_results`.

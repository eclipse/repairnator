# Bears data

`list-of-projects.txt` contains names of repositories hosted on GitHub, which are the projects used to obtain bugs from.

`scanned-builds` folder contains pairs of builds scanned from projects in the file `list-of-projects.txt` during the period of TODO. Each pair of build is supposed to be reprocuded in order to check whether the first build is a buggy build and the second one is its corresponding fixer build.

`branches` folder contains information on branches generated in [Bears collection](https://github.com/fermadeiral/bears-collection) by reprocuding the pairs of scanned builds in the folder `scanned-builds`:

  * `1-reproduced-build-branches` folder contains lists of branches generated from pairs of builds where the buggy build and its fixer build were successfully reproduced.
  * `2-validation-results` folder contains lists of branches (containing in `1-reproduced-build-branches` folder) reproduced by a second time in order to validate them. In these lists, the branches are annotated with the status of the validation, where [OK] indicates successfully validated branches and [FAILURE] indicates unsuccessfully ones.
  * `3-successfully-validated-branches` folder contains lists of branches successfully validated, i.e. with [OK] status in the folder `2-validation-results`.

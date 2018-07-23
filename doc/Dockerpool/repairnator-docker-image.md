# Repairnator docker image

All scripts that are using the pipeline are using a docker-container
created from a docker image of repairnator-pipeline. 

The image used is specified in repairnator.cfg, and is by default
`spirals/repairnator:latest`.

We create this image from the Dockerfile found in
`docker-images/pipeline-dockerimage`, which calls a script that
downloads a jar from Maven based on a version placed in
`version.ini` file.

Thus, for testing one can not rely on this docker-image, since it will
not be up to date. Instead, one has to generate their own image, a
process described below.

## Process of creating your own docker-image

In `/repairnator/repairnator/repairnator-pipeline` run ```mvn
package```. This should create a file
`repairnator-pipeline-[...]-with-dependencies.jar`. Then move and
rename it by running
```
mv repairnator-pipeline-[...]-with-dependencies.jar
../docker-images/pipeline-dockerimage-dev/repairnator-pipeline.jar
mv ../docker-images/pipeline-dockerimage-dev
docker build .
```

The last command will create a docker image and give you an ID for
this build. Replace `spirals/repairnator:latest` with this ID and
the new docker image based on the updated version of the pipeline will
be used.

If not changed in configuration file, repairnator will use the default
docker image, `spirals/repairnator:latest`. So any changes that have
been made locally will not be used.

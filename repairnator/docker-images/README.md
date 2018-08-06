# docker-images

This directory contains the different images Docker used in Repairnator project.
Those images (except the dev one) are self-contained and can be directly build by calling `docker build <path>` without any other command before.
Don't hesitate to have a look on the readme of each image for more information.

The current images are the following ones: 
  - [pipeline-dockerimage](pipeline-dockerimage): the default image for running repairnator;
  - [pipeline-dockerimage-dev](pipeline-dockerimage-dev): this image aims to be used with a custom (in development) repairnator.jar version;
  - [checkbranches-dockerimage](checkbranches-dockerimage): this image is used to check branches of a repository produced by repairnator (read the doc/usage to find more information);
  - [bears-dockerimage](bears-dockerimage): this image is used for BEARS project to compute bugs;
  - [bears-checkbranches-dockerimage](bears-checkbranches-dockerimage): this image is used to check the branches created by BEARS project.
  
  
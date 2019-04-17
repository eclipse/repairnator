# Architecture

This document provides some information about the global architecture of Repairnator through the usage point of view.

## Usage point of view

On the usage point of view, Repairnator is made of 5 components:
  - the docker image,
  - the pipeline,
  - the dockerpool,
  - the scanner,
  - the realtime scanner (RTScanner)
  
### the docker image

The docker image is only an abstraction of the pipeline: 
the idea is to encapsulate the pipeline in an environment in which it will properly work, with the right dependencies, and to be able to use the pipeline in sandboxed environment.

### the pipeline

The pipeline is the most interesting part of Repairnator: it takes as input a Travis CI build ID and tries to replicate the bug and to repair it.
As its name indicates, it's a pipeline of steps, from cloning the repository and building it, to launching the repair tools and pushing a resulting branch.
It contains plenty of options, for notifying the users, creating a pull requests, etc.

### the dockerpool

The dockerpool has been created to manage the scalability of the docker containers: when trying to repair 1000 of builds, we cannot launch all of them at the same time on the same machine.
The idea is to manage a pool of docker containers with the right environment values to pass to the pipeline.
The dockerpool directly operates docker to pull docker images and create, run and delete docker containers.

### the scanner

The scanner is actually the oldest part of Repairnator: it's used to retrieve Travis CI build ID that respects several criteria.
It does not directly operate any other element of Repairnator. It takes as input a list of project to inspect, and produces in output a list of builds to repair.

### the realtime scanner

The RTScanner is the newest part of Repairnator: the idea is to have a daemon that constantly inspect Travis CI for newly failing builds to launch as fast as possible the pipeline to fix them.
The RTScanner then uses some elements from the dockerpool to operate itself the pipeline through its docker image.

# LibRepair

This repository contain all source code for the Project LibRepair.
This project is founded by [INRIA](http://www.inria.fr) and it aims at repairing automatically software bugs on several repositories.
You can find more information on automatic software repair on [our dedicated page](https://team.inria.fr/spirals/research-on-automatic-software-repair/).

# Content

This repository contains three main projects:

  * [jTravis](https://github.com/Spirals-Team/librepair/tree/master/jtravis) is a Java API to Travis. It brings some helpers to parse logs.
  * [RepairNator](https://github.com/Spirals-Team/librepair/tree/master/repairnator) is the main program dedicated to this project: it can automatically scan large set of projects, detect failing builds, reproduce them and try to repair them using our tools
  * [travisFilter](https://github.com/Spirals-Team/librepair/tree/master/travisFilter) is a really small project intented to filter quickly set of Github project to detect if they're using Travis or not.

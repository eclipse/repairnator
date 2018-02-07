# SEIP-2018 "How to design a program repair bot?"

This is an open-science repository which intends to aggregate the data of the paper published at ICSE-SEIP 2018 entitled ["How to Design a Program Repair Bot? Insights from the Repairnator Project"](https://hal.inria.fr/hal-01691496)

For any information, please contact the authors of the paper.

## Structure

The repository contains 2 directories:

1. `data` contains all the raw data that are exploited in the paper
2. `scripts` contains all the scripts used to gather the provided data

## Data

The data are organized in three directories.

 
### 1-all-bugs 

This directory contains all data about section 3.2 in the paper. 
It concerns the list of examinated projects, and the raw data of all tentatively reproduced bugs. 
It also contains aggregated data about number of bugs per projects and overall number of statuses. 

### 2-reproduced-bugs

This directory contains all data about section 4.2 of the paper.
It contains raw data about bugs successfully reproduced as well as aggregated data about kinds of test failures, and reproduced bug by project.
A special file also link the id of a reproduced bug to the URL containing the data of the reproduction.

### 3-patched-bugs

This directory contains all data about section 5.2 of the paper. 
It contains raw data about automatically patched bugs.
A special file also link the id of a patched bug to the URL containing the data of the reproduction and patches.


## Aknowledgement

This research has been supported by InriaHub program and the Wallenberg Autonomous Systems and Software Program (WASP)
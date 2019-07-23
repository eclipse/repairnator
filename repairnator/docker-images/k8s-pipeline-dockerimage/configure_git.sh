#!/usr/bin/env bash
# This initialize git for repairnator and make it possible for the pipeline 
# to push commit to the specified repo according to the provided enviroment variables

git config --global core.whitespace trailing-space,space-before-tab
git config --global apply.whitespace fix

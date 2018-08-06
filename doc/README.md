# Documentation

This directory provides a general documentation about Repairnator project.

## Overview

Repairnator's project aimed at creating a bot to automatically repair failing builds coming from Travis CI.
By repairing we mean proposing patches that make the entire test-suite passing on a given build.

Another project, called BEARS, was built as a collaboration and aimed at creating a database of bugs by mining bugs and patches on Travis CI and Github.

We will mostly rely on Repairnator on this documentation.

## Usage

There are several ways to use Repairnator.
We tried to document some of them in [usage](usage) directory.

## Contributing

Contribution on Repairnator are more than welcome!
A first way to contribute is to look on the label [good-first-issue](https://github.com/Spirals-Team/repairnator/labels/good-first-issue).

Another way for contributing is to add a new program repair tool in Repairnator: [we provided a guide to help us](contributing/add-repair-tool.md).

## Chores

As part of using Repairnator, you might need to do some chores, like managing a MongoDB database.
We provided some documentation [about backups](chore/managedb.md) and [about MongoDB collection schema](chore/mongo).

## Program repair tools used in Repairnator

The following is the list of program repair tools currently supported in Repairnator:
  - Nopol
  - NPEFix
  - Astor (JGenProg)
  - Astor (JKali)
  - Astor (JMut)
  - AssertFixer

# maven-repair: a Maven plugin for executing automated program repair tools on Maven projects 

[![Build Status](https://travis-ci.org/Spirals-Team/maven-repair.svg?branch=master)](https://travis-ci.org/Spirals-Team/maven-repair)

The goal of this projects is is to simplify automatic repair on Maven projects.


## Automatic Repair Techniques

- [X] NPEFix
- [X] Nopol
- [X] DynaMoth
- [X] jGenProg
- [X] jKali
- [X] cardumen

## Install

### Manual install

```bash
git clone https://github.com/Spirals-Team/maven-repair
cd maven-repair
mvn install
```

### Maven

```bash
mvn org.apache.maven.plugins:maven-dependency-plugin:2.1:get \
    -DrepoUrl=https://tdurieux.github.io/maven-repository/snapshots/ \
    -Dartifact=fr.inria.gforge.spirals:repair-maven-plugin:1.4-SNAPSHOT
``` 

## Usage

```bash
cd /somewhere/my-project-with-failing-tests

# check the failing tests
mvn test -DtrimStackTrace=false

# look for patches with Nopol
mvn fr.inria.gforge.spirals:repair-maven-plugin:nopol

# look for patches with NpeFix
mvn fr.inria.gforge.spirals:repair-maven-plugin:npefix
```

## Output

```
# the patches are in target
cat target/npefix/patches.json

cat target/nopol/output.json
```

## Output Format

### NPEFix
```js
{
  "executions": [
    /* all laps */
    {
      "result": {
        "error": "<the exception>",
        "type": "<the oracle type>",
        "success": true
      },
      /* all decisions points */
      "locations": [{
        "sourceEnd": 12234,
        "executionCount": 0,
        "line": 352,
        "class": "org.apache.commons.collections.iterators.CollatingIterator",
        "sourceStart": 12193
      }],
      /* the runned test */
      "test": {
        "name": "testNullComparator",
        "class": "org.apache.commons.collections.iterators.TestCollatingIterator"
      },
      /* all decision made during the laps */
      "decisions": [{
        /* the location of the laps */
        "location": {
          "sourceEnd": 12234,
          "line": 352,
          "class": "org.apache.commons.collections.iterators.CollatingIterator",
          "sourceStart": 12193
        },
        /* the value used by the decision */
        "value": {
          "variableName": "leastObject",
          "value": "leastObject",
          "type": "int"
        },
        /* the value of the epsilon */
        "epsilon": 0.4,
        // the name of the strategy
        "strategy": "Strat4 VAR",
        "used": true,
        /* the decision type (new, best, random) */
        "decisionType": "new"
      }],
      "startDate": 1453918743999,
      "endDate": 1453918744165,
      "metadata": {"seed": 10}
    },
    ...
  ],
  "searchSpace": [
    /* all detected decisions */
    {
      "location": {
        "sourceEnd": 12234,
        "line": 352,
        "class": "org.apache.commons.collections.iterators.CollatingIterator",
        "sourceStart": 12193
      },
      "value": {
        "value": "1",
        "type": "int"
      },
      "epsilon": 0,
      "strategy": "Strat4 NEW",
      "used": false,
      "decisionType": "random"
    },
    ...
  ],
  "date": "Wed Jan 27 19:19:37 CET 2016"
}
```
# History

For history on the project, visit https://github.com/Spirals-Team/maven-repair/

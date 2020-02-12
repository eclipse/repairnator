# maven-repair: a Maven plugin for executing automated program repair tools on Maven projects 

The goal of this projects is is to simplify automatic repair on Maven projects.

## Install

There are released versions on Maven Central, but they may not be up-to-date: https://search.maven.org/search?q=a:repair-maven-plugin

### Manual install

```bash
git clone https://github.com/eclipse/repairnator
cd src/maven-repair
mvn install
```

## Usage to repair a NullPointerException (NpeFix)

```bash
git clone https://github.com/Spirals-Team/npe-dataset/

# this is a real world NPE for Apache Commons Lang
cd npe-dataset/lang-304

# alternatively you can enter your own project at a commit with an NPE

# check the failing tests
mvn test -DtrimStackTrace=false

# look for patches with NpeFix
mvn fr.inria.gforge.spirals:repair-maven-plugin:npefix
```

## Usage to repair a condition bug (Nopol)

```bash
git clone https://github.com/SpoonLabs/nopol-experiments

# this is a real world NPE for Apache Commons Lang
cd nopol-experiments/dataset/cl5

# alternatively you can enter your own project at a commit with a condition bug

# check the failing tests
mvn test -DtrimStackTrace=false

# look for patches with Nopol
mvn fr.inria.gforge.spirals:repair-maven-plugin:nopol
```

Maven-repair output for Nopol:

```
[INFO] ----PATCH FOUND----
[INFO] className.length() == 0
[INFO] Nb test that executes the patch: 37
[INFO] org.apache.commons.lang.ClassUtils:258: CONDITIONAL
[INFO] --- a/src/java/org/apache/commons/lang/ClassUtils.java
+++ b/src/java/org/apache/commons/lang/ClassUtils.java
@@ -257,3 +257,3 @@
     public static String getPackageName(String className) {
-        if (className == null) {
+        if (className.length() == 0) {
             return StringUtils.EMPTY;

Nopol executed after: 14233 ms.

```



## Output

```
# the patches are in target
cat target/npefix/patches.json

cat target/nopol/output.json
```

## Output Format

### NPEFix output in a JSON file in `target/`
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

## Automatic Repair Techniques Supported

- [X] NPEFix
- [X] Nopol
- [X] DynaMoth
- [X] jGenProg
- [X] jKali
- [X] cardumen

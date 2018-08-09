# Add your own repair tool

## Overview

Integrating a new repair tool is easy:
  1. Deploy your repair tool on Maven Central
  1. Add a dependency to it in the Repairnator `pom.xml`
  1. You must create a new `RepairStep` class in Repairnator.

## Deploy your repair tool on Maven Central

See [Guide to uploading artifacts to the Central Repository](https://maven.apache.org/repository/guide-central-repository-upload.html)

## Add the dependency in pom.xml

First, add the dependency to your repair tool Java API in `repairnator-pipeline/pom.xml`.  All the changes you will make are in the module `repairnator-pipeline`.

**WARNING**: 

* if the dependency is not on Maven Central, you need to add a new repository in the `pom.xml` (discouraged). 
* if your tool is available through a Maven Plugin, the dependency is not needed in the `pom.xml` file, but the plugin **must be** released on Maven Central.

## Create your own RepairStep Java Class

Then, Yyou create a new Java class in package `fr.inria.spirals.repairnator.process.step.repair`. This class must inherit from `fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep`.

* This class must declare a public constructor without any parameter.
* The class must be declared with its fully qualified name in `repairnator-pipeline/src/main/resources/META-INF/services/fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep`.
 
 See for example [AssertFixerRepair.java](https://github.com/Spirals-Team/repairnator/blob/master/repairnator/repairnator-pipeline/src/main/java/fr/inria/spirals/repairnator/process/step/repair/AssertFixerRepair.java)
 
(Technical note: Repairnator use a Java SPI to automatically discover the available repair tools step.)

### Implementing the RepairStep Java Class

When you inherit from the `AbstractRepairStep` class, you first need to implement the abstract method `getRepairToolName()`: this method should only returns a String with the name of the tool you are bringing.

Then you need to override and implement the method `businessExecute()`. This method is automatically called to execute the repair step: it's always called after the failing builds has been reproduced, and thus you can get access to information about the test failures and about some paths of the project.

#### Accessing the context information with JobStatus

Most of the information you need to get access to are stored in a class named `fr.inria.spirals.repairnator.process.inspectors.JobStatus` and is available through the inspector from the RepairStep:
`this.getInspector().getJobStatus()`.

**Get access to the test results**: If you need to analyze the results of the test, you can get access to them through the class `JobStatus` with a method named `getFailureLocations()`:

```java
JobStatus jobStatus = this.getInspector().getJobStatus();
Collection<FailureLocations> failureLocations = jobStatus.getFailureLocations();

for (FailureLocation failureLocation : failureLocations) {
    // if there is test failures
    if (this.getNbFailures() + this.getNbErrors() > 0) {
        String className = failureLocation.getClassName();
        Set<String> failingMethods = failureLocation.getFailingMethods();
        List<FailureType> failureTypes = failureLocation.getFailureTypes();
    }
}
```
Have a look on `fr.inria.spirals.repairnator.process.testinformation.FailureLocation` to get more information about this class.

**Get access to source and test directories** Several paths are available from the `JobStatus` when you execute a RepairStep:
  - source directory: `getRepairSourceDir()`: return an array of file (`File[]`)
  - test directory: `getTestDir()`: also returns an array of file (`File[]`) (WARNING: its value might be null)
  - classpath to execute the project: `getRepairClassPath()`: returns a list of URLs (`List<URL>`) (`target/classes` and `target/test-classes` are also added)
  - directory of the pom file: `getPomDirPath()`: returns a string
  - in case of a multi-module Maven project, path of the failing module: `getFailingModulePath()`: returns a string

You can also directly get access to the path of the pom file of the project, through the RepairStep: `this.getPom()`.


#### Creating the output information with StepStatus

The method `businessExecute` should return a `StepStatus`: this status allows us to know if the step has run well or if there was a problem when running it.
You can directly make those return calls in case of success or failure:
  - `return StepStatus.buildSuccess(this)`
  - `return StepStatus.buildError(this, "message explaining the error")`
  
Have a look on `fr.inria.spirals.repairnator.process.inspectors.StepStatus` to get more information about this class.

#### Recording patches

When the tool manage to produce a patch, this one must be recorded in database and send by email to the Repairnator administrators to propose a pull request.
In order to do so, you first need to create an object `fr.inria.spirals.repairnator.process.inspectors.RepairPatch` for each patch generated.
This object takes as input:
  - the name of the tool used to produce it,
  - the path of the file on which the patch should be applied
  - the diff of the patch
  
Then all patches created in a RepairStep are submitted stimultaneously with the method: `this.recordPatches(List<RepairPatch>)`.
This method returns nothing. It will record all patches in database and send automatically an email.

In most cases, even if no patch has been generated, the repair tools create some diagnosis information that might be interesting for the research community and for program repair developers.
In order to record all data created during an execution of a repair tool, a method `this.recordToolDiagnostic(JsonElement)` is available.
This method takes as input a `JsonElement` which might be a `JsonObject` or a `JsonArray` and this element will be directly stored in database for further investigation.

### How to test the Repairnator integration

We advise you the following process in order to ease your life when testing. 

* Create a specific Github repository and setup Travis CI on it. 
* Then put some specific examples of codes that are buggy, but that you program repair tools know how to fix.
* Then when a build is triggered on Travis CI and marked as failing, get its id (available through its URL), and run Repairnator - configured with your own repair tool - with the build id in argument.
* If everything's right, you should get some patches for fixing your own code. 


### Managing errors

At each point, if you need to handle an exception or an error without exception, you can use the dedicated method:

```java
this.addStepError("my error message");

try {
    // some stuff
} catch (Exception e) {
    this.addStepError("my error message", e);
}
```

Errors are logged with error status, and they're also recorded in the database for further investigation.
  
#### Execute a Maven Goal

If your program repair is available through a Maven Plugin, you can execute it in the step, using our own Maven invoker: 
```java
String goalOfMyPlugin = "my.plugin:mygoal";
Properties properties = new Properties();
// put some properties if needed
boolean enableHandlers = true; // if false logs are mute, else they will be displayed 
MavenHelper mavenHelper = new MavenHelper(this.getPom(), goalOfMyPlugin, properties, this.getRepairToolName(), this.getInspector(), enableHandlers);
int status = MavenHelper.MAVEN_ERROR;
try {
    status = mavenHelper.run();
} catch (InterruptedException e) {
    this.addStepError("Error while executing Maven goal", e);
}

if (status == MAVEN_ERROR) {
    // some stuff
} else {
    // success
}
```

**WARNING**: The Maven Plugin must necessarily be released on Maven Central to be available in Repairnator.

## Submitting your code

Now you're done! We're super happy to receive your pull request explaining which tool you are bringing in Repairnator
Thanks! :+1::+1:

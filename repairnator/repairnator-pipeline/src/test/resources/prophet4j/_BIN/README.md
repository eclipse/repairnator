Prophet4J is inside the Coming lib, and is introduced to compute OverfittingScores for patches.

This folder contains necessary resources for Prophet4J. There are two same folders in total, one is at _src/main/resources_ and the other one is at _src/test/resources_. I had not found one correct way to import resources files to make Prophet4J work smoothly as one part of Maven lib. In ideal cases, we do not need these two folders at all.

If you fixed this issue, please run this test file `src/test/java/fr/inria/spirals/repairnator/process/inspectors/TestJobStatus.java` to check. Or maybe later I will do it myself.

How to fix this issue?

1. the issue is at L11 at _src/main/java/fr/inria/prophet4j/utility/Support.java_ of the [Coming Project]()
That line of code looks like `public static final String PROPHET4J_DIR = Support.class.getClassLoader().getResource("").getPath() + "prophet4j/";`
2. rewrite this line of code to make everything prefect
3. release one new version of Coming lib to Maven
4. run the test file named as `TestJobStatus.java` to check
5. remove two `prophet4j` folders locating at _src/main/resources_ and _src/test/resources_

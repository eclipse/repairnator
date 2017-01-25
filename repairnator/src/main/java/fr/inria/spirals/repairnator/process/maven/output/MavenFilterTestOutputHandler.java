package fr.inria.spirals.repairnator.process.maven.output;

import fr.inria.spirals.repairnator.process.ProjectInspector;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by urli on 23/01/2017.
 */
public class MavenFilterTestOutputHandler extends MavenFilterOutputHandler {

    private static final String MVN_TEST_PATTERN = "^Tests run: (\\d*), Failures: (\\d*), Errors: (\\d*), Skipped: (\\d*)(, Time elapsed: ([\\d\\.]*) sec (<<< FAILURE! )?- in ([\\w\\.]*))?$";
    private static final String MVN_GOAL_FAILED_WITH_TEST_FAILURE = "There are test failures.";

    private Pattern mvnGoalFailPattern;
    private Pattern mvnTestPattern;
    private int runningTests, failingTests, erroringTests, skippingTests;
    private float totalElapsedTime;
    private List<String> failingClasses;
    private boolean failingWithTest;

    public MavenFilterTestOutputHandler(ProjectInspector inspector, String name) {
        super(inspector, name);
        this.mvnTestPattern = Pattern.compile(MVN_TEST_PATTERN);
        this.mvnGoalFailPattern = Pattern.compile(MVN_GOAL_FAILED_WITH_TEST_FAILURE);

        this.runningTests = 0;
        this.failingTests = 0;
        this.erroringTests = 0;
        this.skippingTests = 0;
        this.totalElapsedTime = 0;
        this.failingClasses = new ArrayList<String>();
    }

    public int getRunningTests() {
        return runningTests;
    }

    public int getFailingTests() {
        return failingTests;
    }

    public int getErroringTests() {
        return erroringTests;
    }

    public int getSkippingTests() {
        return skippingTests;
    }

    public float getTotalElapsedTime() {
        return totalElapsedTime;
    }

    public List<String> getFailingClasses() {
        return failingClasses;
    }

    public boolean isFailingWithTest() {
        return failingWithTest || failingTests > 0;
    }

    @Override
    public void consumeLine(String s) {
        super.consumeLine(s);

        Matcher matcher = this.mvnTestPattern.matcher(s.trim());

        if (matcher.matches()) {
            this.runningTests += Integer.parseInt(matcher.group(1));
            int failing = Integer.parseInt(matcher.group(2));

            this.failingTests += failing;
            this.erroringTests += Integer.parseInt(matcher.group(3));
            this.skippingTests += Integer.parseInt(matcher.group(4));

            if (matcher.group(5) != null) {
                this.totalElapsedTime += Float.parseFloat(matcher.group(6));

                if (failing > 0) {
                    this.failingClasses.add(matcher.group(8));
                    this.failingWithTest = true;
                }
            }
        }

        matcher = this.mvnGoalFailPattern.matcher(s.trim());
        if (matcher.matches()) {
            this.failingWithTest = true;
        }
    }
}

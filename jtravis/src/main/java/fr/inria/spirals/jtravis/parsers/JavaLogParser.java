package fr.inria.spirals.jtravis.parsers;

import fr.inria.spirals.jtravis.entities.TestsInformation;

/**
 * Created by urli on 22/02/2017.
 */
public abstract class JavaLogParser {

    protected int runningTests;
    protected int erroredTests;
    protected int skippingTests;
    protected int failingTests;
    protected int passingTests;

    public abstract TestsInformation parseLog(TravisFold outOfFolds);

    protected TestsInformation createTestInformation() {
        TestsInformation result = new TestsInformation();

        result.setErrored(this.erroredTests);
        result.setRunning(this.runningTests);
        result.setSkipping(this.skippingTests);
        result.setPassing(this.passingTests);
        result.setFailing(this.failingTests);

        return result;
    }
}

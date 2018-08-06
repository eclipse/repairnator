package fr.inria.spirals.repairnator.process.inspectors.properties.tests;

import java.util.HashSet;
import java.util.Set;

public class Tests {

    private String failingModule;
    private OverallMetrics overallMetrics;
    private Set<FailingClass> failingClasses;
    private Set<FailureDetail> failureDetails;

    public Tests() {
        this.overallMetrics = new OverallMetrics();
        this.failingClasses = new HashSet<>();
        this.failureDetails = new HashSet<>();
    }

    public String getFailingModule() {
        return failingModule;
    }

    public void setFailingModule(String failingModule) {
        this.failingModule = failingModule;
    }

    public OverallMetrics getOverallMetrics() {
        return overallMetrics;
    }

    public Set<FailingClass> getFailingClasses() {
        return failingClasses;
    }

    public FailingClass addFailingClass(String failingClassName) {
        for (FailingClass failingClass : this.failingClasses) {
            if (failingClass.getTestClass().equals(failingClassName)) {
                return failingClass;
            }
        }
        FailingClass failingClass = new FailingClass(failingClassName);
        this.failingClasses.add(failingClass);
        return failingClass;
    }

    public Set<FailureDetail> getFailureDetails() {
        return failureDetails;
    }

    public void addFailureDetail(FailureDetail failureDetail) {
        this.failureDetails.add(failureDetail);
    }

}

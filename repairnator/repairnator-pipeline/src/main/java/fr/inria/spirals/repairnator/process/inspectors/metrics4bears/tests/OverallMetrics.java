package fr.inria.spirals.repairnator.process.inspectors.metrics4bears.tests;

import java.util.HashSet;
import java.util.Set;

public class OverallMetrics {

    private int numberExecuted;
    private int numberFailed;
    private int numberErrored;
    private int numberSkipped;
    private int numberPassed;
    private Set<Failure> failures;

    public OverallMetrics() {
        this.failures = new HashSet<>();
    }

    public int getNumberExecuted() {
        return numberExecuted;
    }

    public void setNumberExecuted(int numberExecuted) {
        this.numberExecuted = numberExecuted;
    }

    public int getNumberFailed() {
        return numberFailed;
    }

    public void setNumberFailed(int numberFailed) {
        this.numberFailed = numberFailed;
    }

    public int getNumberErrored() {
        return numberErrored;
    }

    public void setNumberErrored(int numberErrored) {
        this.numberErrored = numberErrored;
    }

    public int getNumberSkipped() {
        return numberSkipped;
    }

    public void setNumberSkipped(int numberSkipped) {
        this.numberSkipped = numberSkipped;
    }

    public int getNumberPassed() {
        return numberPassed;
    }

    public void setNumberPassed(int numberPassed) {
        this.numberPassed = numberPassed;
    }

    public Set<Failure> getFailures() {
        return failures;
    }

    public void addFailure(Failure failure) {
        this.failures.add(failure);
    }

}

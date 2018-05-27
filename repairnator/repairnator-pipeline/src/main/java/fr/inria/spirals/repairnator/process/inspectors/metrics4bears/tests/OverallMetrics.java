package fr.inria.spirals.repairnator.process.inspectors.metrics4bears.tests;

import java.util.HashSet;
import java.util.Set;

public class OverallMetrics {

    private int numberRunning;
    private int numberPassing;
    private int numberFailing;
    private int numberErroring;
    private int numberSkipping;
    private Set<Failure> failures;

    public OverallMetrics() {
        this.failures = new HashSet<>();
    }

    public int getNumberRunning() {
        return numberRunning;
    }

    public void setNumberRunning(int numberRunning) {
        this.numberRunning = numberRunning;
    }

    public int getNumberPassing() {
        return numberPassing;
    }

    public void setNumberPassing(int numberPassing) {
        this.numberPassing = numberPassing;
    }

    public int getNumberFailing() {
        return numberFailing;
    }

    public void setNumberFailing(int numberFailing) {
        this.numberFailing = numberFailing;
    }

    public int getNumberErroring() {
        return numberErroring;
    }

    public void setNumberErroring(int numberErroring) {
        this.numberErroring = numberErroring;
    }

    public int getNumberSkipping() {
        return numberSkipping;
    }

    public void setNumberSkipping(int numberSkipping) {
        this.numberSkipping = numberSkipping;
    }

    public Set<Failure> getFailures() {
        return failures;
    }

    public void addFailure(Failure failure) {
        this.failures.add(failure);
    }

}

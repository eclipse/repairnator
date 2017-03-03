package fr.inria.spirals.repairnator.process.testinformation;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent the location of a failure for Repair Tools. For now only Nopol is
 * using and it's repairing Test Classes, so we only consider that information
 */
public class FailureLocation {
    private String className;
    private List<FailureType> failures;
    private int nbFailures;
    private int nbErrors;

    public FailureLocation(String className) {
        this.className = className;
        this.failures = new ArrayList<>();
    }

    public String getClassName() {
        return className;
    }

    public void addFailure(FailureType failure) {
        this.failures.add(failure);
        if (failure.isError()) {
            nbErrors++;
        } else {
            nbFailures++;
        }
    }

    public List<FailureType> getFailures() {
        return failures;
    }

    public int getNbFailures() {
        return nbFailures;
    }

    public int getNbErrors() {
        return nbErrors;
    }

    public boolean isError() {
        return (nbErrors > nbFailures);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FailureLocation that = (FailureLocation) o;

        return className != null ? className.equals(that.className) : that.className == null;
    }

    @Override
    public int hashCode() {
        return className != null ? className.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Test case: " + className + " (Fail: " + nbFailures + "; Error: " + nbErrors + ")";
    }
}

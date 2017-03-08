package fr.inria.spirals.repairnator.process.testinformation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represent the location of a failure for Repair Tools. For now only Nopol is
 * using and it's repairing Test Classes, so we only consider that information
 */
public class FailureLocation {
    private String className;
    private Set<String> failingMethods;
    private Set<String> erroringMethods;
    private List<FailureType> failures;
    private int nbFailures;
    private int nbErrors;

    public FailureLocation(String className) {
        this.className = className;
        this.failures = new ArrayList<>();
        this.failingMethods = new HashSet<String>();
        this.erroringMethods = new HashSet<String>();
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

    public void addFailingMethod(String failingMethod) {
        this.failingMethods.add(failingMethod);
    }

    public void addErroringMethod(String erroringMethod) {
        this.erroringMethods.add(erroringMethod);
    }

    public Set<String> getFailingMethods() {
        return failingMethods;
    }

    public Set<String> getErroringMethods() {
        return erroringMethods;
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
    public String toString() {
        return "FailureLocation{" +
                "className='" + className + '\'' +
                ", failingMethods=" + failingMethods +
                ", erroringMethods=" + erroringMethods +
                ", failures=" + failures +
                ", nbFailures=" + nbFailures +
                ", nbErrors=" + nbErrors +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FailureLocation that = (FailureLocation) o;

        if (nbFailures != that.nbFailures) return false;
        if (nbErrors != that.nbErrors) return false;
        if (className != null ? !className.equals(that.className) : that.className != null) return false;
        if (failingMethods != null ? !failingMethods.equals(that.failingMethods) : that.failingMethods != null) return false;
        if (erroringMethods != null ? !erroringMethods.equals(that.erroringMethods) : that.erroringMethods != null) return false;
        return failures != null ? failures.equals(that.failures) : that.failures == null;
    }

    @Override
    public int hashCode() {
        int result = className != null ? className.hashCode() : 0;
        result = 31 * result + (failingMethods != null ? failingMethods.hashCode() : 0);
        result = 31 * result + (erroringMethods != null ? erroringMethods.hashCode() : 0);
        result = 31 * result + (failures != null ? failures.hashCode() : 0);
        result = 31 * result + nbFailures;
        result = 31 * result + nbErrors;
        return result;
    }
}

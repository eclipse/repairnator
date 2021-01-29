package fr.inria.spirals.repairnator.process.testinformation;

import java.util.*;

/**
 * Represent the location of a failure for Repair Tools. For now only Nopol is
 * using and it's repairing Test Classes, so we only consider that information
 */
public class FailureLocation {
    private String className;
    private HashMap<String, List<FailureType>> failingMethods;
    private HashMap<String, List<FailureType>> erroringMethods;
    private int nbFailures;
    private int nbErrors;

    public FailureLocation(String className) {
        this.className = className;
        this.failingMethods = new HashMap<>();
        this.erroringMethods = new HashMap<>();
    }

    public String getClassName() {
        return className;
    }

    public void addFailingMethod(String failingMethod, FailureType failure) {
        this.nbFailures++;
        if (this.failingMethods.containsKey(failingMethod)) {
            this.failingMethods.get(failingMethod).add(failure);
        } else {
            this.failingMethods.put(failingMethod, Collections.singletonList(failure));
        }
    }

    public void addErroringMethod(String erroringMethod, FailureType failure) {
        this.nbErrors++;
        if (this.erroringMethods.containsKey(erroringMethod)) {
            this.erroringMethods.get(erroringMethod).add(failure);
        } else {
            this.erroringMethods.put(erroringMethod, Collections.singletonList(failure));
        }
    }

    public Set<String> getFailingMethods() {
        return failingMethods.keySet();
    }

    public Set<String> getErroringMethods() {
        return erroringMethods.keySet();
    }

    public Map<String, List<FailureType>> getFailingMethodsAndFailures() {
        return failingMethods;
    }

    public Map<String, List<FailureType>> getErroringMethodsAndFailures() {
        return erroringMethods;
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
                ", nbFailures=" + nbFailures +
                ", nbErrors=" + nbErrors +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FailureLocation that = (FailureLocation) o;
        return nbFailures == that.nbFailures &&
                nbErrors == that.nbErrors &&
                Objects.equals(className, that.className) &&
                Objects.equals(failingMethods, that.failingMethods) &&
                Objects.equals(erroringMethods, that.erroringMethods);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, failingMethods, erroringMethods, nbFailures, nbErrors);
    }
}

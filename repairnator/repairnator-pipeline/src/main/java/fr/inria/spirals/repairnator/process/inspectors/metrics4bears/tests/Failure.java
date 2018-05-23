package fr.inria.spirals.repairnator.process.inspectors.metrics4bears.tests;

public class Failure {

    private String failureName;
    private boolean isError;
    private int occurrences;

    public Failure() {}

    public String getFailureName() {
        return failureName;
    }

    public void setFailureName(String failureName) {
        this.failureName = failureName;
    }

    public boolean getIsError() {
        return isError;
    }

    public void setIsError(boolean isError) {
        isError = isError;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(int occurrences) {
        this.occurrences = occurrences;
    }

}

package fr.inria.spirals.repairnator.process.inspectors.properties.tests;

public class Failure {

    private String failureName;
    private boolean isError;
    private int occurrences;

    public Failure(String failureName, boolean isError) {
        this.failureName = failureName;
        this.isError = isError;
    }

    public String getFailureName() {
        return failureName;
    }

    public boolean getIsError() {
        return isError;
    }

    public int getOccurrences() {
        return occurrences;
    }

    public void setOccurrences(int occurrences) {
        this.occurrences = occurrences;
    }

}

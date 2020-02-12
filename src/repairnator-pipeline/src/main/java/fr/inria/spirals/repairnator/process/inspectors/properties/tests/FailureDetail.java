package fr.inria.spirals.repairnator.process.inspectors.properties.tests;

public class FailureDetail {

    private String testClass;
    private String testMethod;
    private String failureName;
    private String detail;
    private boolean isError;

    public FailureDetail() {}

    public String getTestClass() {
        return testClass;
    }

    public void setTestClass(String testClass) {
        this.testClass = testClass;
    }

    public String getTestMethod() {
        return testMethod;
    }

    public void setTestMethod(String testMethod) {
        this.testMethod = testMethod;
    }

    public String getFailureName() {
        return failureName;
    }

    public void setFailureName(String failureName) {
        this.failureName = failureName;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public boolean getIsError() {
        return isError;
    }

    public void setError(boolean error) {
        isError = error;
    }
}

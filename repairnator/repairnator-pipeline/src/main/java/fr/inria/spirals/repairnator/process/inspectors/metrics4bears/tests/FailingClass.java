package fr.inria.spirals.repairnator.process.inspectors.metrics4bears.tests;

public class FailingClass {

    private String testClass;
    private int numberExecuted;
    private int numberFailed;
    private int numberErrored;
    private int numberPassed;
    private int numberSkipped;

    public FailingClass(String testClass) {
        this.testClass = testClass;
    }

    public String getTestClass() {
        return testClass;
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

    public int getNumberPassed() {
        return numberPassed;
    }

    public void setNumberPassed(int numberPassed) {
        this.numberPassed = numberPassed;
    }

    public int getNumberSkipped() {
        return numberSkipped;
    }

    public void setNumberSkipped(int numberSkipped) {
        this.numberSkipped = numberSkipped;
    }
}

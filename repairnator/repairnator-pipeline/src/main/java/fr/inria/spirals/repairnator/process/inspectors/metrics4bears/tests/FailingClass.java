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

    public void incNumberExecuted() {
        this.numberExecuted++;
    }

    public int getNumberFailed() {
        return numberFailed;
    }

    public void incNumberFailed() {
        this.numberFailed++;
    }

    public int getNumberErrored() {
        return numberErrored;
    }

    public void incNumberErrored() {
        this.numberErrored++;
    }

    public int getNumberPassed() {
        return numberPassed;
    }

    public void incNumberPassed() {
        this.numberPassed++;
    }

    public int getNumberSkipped() {
        return numberSkipped;
    }

    public void incNumberSkipped() {
        this.numberSkipped++;
    }
}

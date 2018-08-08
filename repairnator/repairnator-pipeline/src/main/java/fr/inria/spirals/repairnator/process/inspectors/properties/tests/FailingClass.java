package fr.inria.spirals.repairnator.process.inspectors.properties.tests;

public class FailingClass {

    private String testClass;
    private int numberRunning;
    private int numberPassing;
    private int numberFailing;
    private int numberErroring;
    private int numberSkipping;

    public FailingClass(String testClass) {
        this.testClass = testClass;
    }

    public String getTestClass() {
        return testClass;
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
}

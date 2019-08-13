package fr.inria.spirals.repairnator.process.step.repair.sequencer;

// todo rewrite
public class SequencerResult {
    private String testClass;
    private String testMethod;
    private boolean success;
    private String exceptionMessage;
    private String diff;
    private String filePath;

    public SequencerResult(String testClass, String testMethod) {
        this.testClass = testClass;
        this.testMethod = testMethod;
    }

    public String getTestClass() {
        return this.testClass;
    }

    public String getTestMethod() {
        return this.testMethod;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getExceptionMessage() {
        return this.exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public String getDiff() {
        return this.diff;
    }

    public void setDiff(String diff) {
        this.diff = diff;
    }

    public String getFilePath() {
        return this.filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

}

package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.states.PipelineState;

/**
 * This class defines how an AbstractStep has been executed.
 * It gives a status between success, failure or skipped, but also some more information in a diagnostic.
 */
public class StepStatus {
    public enum StatusKind {
        SUCCESS,
        FAILURE,
        SKIPPED
    }

    private StatusKind status;
    private String diagnostic;
    private AbstractStep step;

    public StepStatus(AbstractStep step, StatusKind status, String diagnostic) {
        this.step = step;
        this.status = status;
        this.diagnostic = diagnostic;
    }

    public static StepStatus buildSuccess(AbstractStep step) {
        return new StepStatus(step, StatusKind.SUCCESS, "");
    }

    public static StepStatus buildError(AbstractStep step, PipelineState diagnostic) {
        return new StepStatus(step, StatusKind.FAILURE, diagnostic.name());
    }

    public static StepStatus buildSkipped(AbstractStep step, String reason) {
        return new StepStatus(step, StatusKind.SKIPPED, reason);
    }

    public static StepStatus buildSkipped(AbstractStep step) {
        return new StepStatus(step, StatusKind.SKIPPED, "");
    }

    public AbstractStep getStep() {
        return step;
    }

    public String getDiagnostic() {
        return diagnostic;
    }

    public StatusKind getStatus() {
        return status;
    }

    /**
     * @return true only if the status kind is a success
     */
    public boolean isSuccess() {
        return this.getStatus() == StatusKind.SUCCESS;
    }

    public String toString() {
        String suffix = (this.diagnostic.isEmpty()) ? "" : " ( " + this.diagnostic + " )";
        return "[ " + this.step.getName() + " : " + this.status.name() + suffix + " ]";
    }
}

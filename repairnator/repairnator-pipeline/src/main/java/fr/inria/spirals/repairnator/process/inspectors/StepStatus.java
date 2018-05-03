package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.states.PipelineState;

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

    public boolean isSuccess() {
        return this.getStatus() == StatusKind.SUCCESS;
    }

    public String toString() {
        String suffix = (this.diagnostic.isEmpty()) ? "" : " ( " + this.diagnostic + " )";
        return "[ " + this.step.getName() + " : " + this.status.name() + suffix + " ]";
    }
}

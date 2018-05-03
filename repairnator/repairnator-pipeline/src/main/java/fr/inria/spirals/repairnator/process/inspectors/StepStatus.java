package fr.inria.spirals.repairnator.process.inspectors;

import java.util.Objects;

public class StepStatus {
    public enum StatusKind {
        SUCCESS,
        FAILURE,
        SKIPPED
    }

    private StatusKind status;
    private String diagnostic;

    public StepStatus(StatusKind status, String diagnostic) {
        this.status = status;
        this.diagnostic = diagnostic;
    }

    public static StepStatus buildSuccess() {
        return new StepStatus(StatusKind.SUCCESS, "");
    }

    public static StepStatus buildError(String diagnostic) {
        return new StepStatus(StatusKind.FAILURE, diagnostic);
    }

    public static StepStatus buildSkipped() {
        return new StepStatus(StatusKind.SKIPPED, "");
    }

    public static StepStatus buildSkipped(String reason) {
        return new StepStatus(StatusKind.SKIPPED, reason);
    }

    public String getDiagnostic() {
        return diagnostic;
    }

    public StatusKind getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final StepStatus that = (StepStatus) o;
        return status == that.status &&
                Objects.equals(diagnostic, that.diagnostic);
    }

    @Override
    public int hashCode() {

        return Objects.hash(status, diagnostic);
    }

    public boolean isSuccess() {
        return this.getStatus() == StatusKind.SUCCESS;
    }
}

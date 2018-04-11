package fr.inria.spirals.repairnator;

import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;

import java.util.Objects;

/**
 * Created by fernanda on 24/02/17.
 */
public class BuildToBeInspected {

    private Build patchedBuild;
    private Build buggyBuild;
    // For RepairNator, the status is about the build, and for Bears it is about the previous build
    private ScannedBuildStatus status;
    private String runId;

    public BuildToBeInspected(Build buggyBuild, Build patchedBuild, ScannedBuildStatus status, String runId) {
        this.patchedBuild = patchedBuild;
        this.buggyBuild = buggyBuild;
        this.status = status;
        this.runId = runId;
    }

    public Build getPatchedBuild() {
        return patchedBuild;
    }

    public Build getBuggyBuild() {
        return buggyBuild;
    }

    public ScannedBuildStatus getStatus() {
        return status;
    }

    public String getRunId() {
        return runId;
    }

    @Override
    public String toString() {
        return "BuildToBeInspected{" +
                "patchedBuild=" + patchedBuild +
                ", buggyBuild=" + buggyBuild +
                ", status=" + status +
                ", runId='" + runId + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final BuildToBeInspected that = (BuildToBeInspected) o;
        return Objects.equals(patchedBuild, that.patchedBuild) &&
                Objects.equals(buggyBuild, that.buggyBuild) &&
                status == that.status &&
                Objects.equals(runId, that.runId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(patchedBuild, buggyBuild, status, runId);
    }
}

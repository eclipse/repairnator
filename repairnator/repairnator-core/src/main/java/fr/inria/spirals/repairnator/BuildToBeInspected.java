package fr.inria.spirals.repairnator;

import fr.inria.spirals.jtravis.entities.Build;

/**
 * Created by fernanda on 24/02/17.
 */
public class BuildToBeInspected {

    private Build build;
    private Build previousBuild;
    // For RepairNator, the status is about the build, and for Bears it is about the previous build
    private ScannedBuildStatus status;
    private String runId;

    public BuildToBeInspected(Build build, ScannedBuildStatus status, String runId) {
        this.build = build;
        this.previousBuild = null;
        this.status = status;
        this.runId = runId;
    }

    public BuildToBeInspected(Build build, Build previousBuild, ScannedBuildStatus status, String runId) {
        this.build = build;
        this.previousBuild = previousBuild;
        this.status = status;
        this.runId = runId;
    }

    public Build getBuild() {
        return build;
    }

    public Build getPreviousBuild() {
        return previousBuild;
    }

    public ScannedBuildStatus getStatus() {
        return status;
    }

    public String getRunId() {
        return runId;
    }
}

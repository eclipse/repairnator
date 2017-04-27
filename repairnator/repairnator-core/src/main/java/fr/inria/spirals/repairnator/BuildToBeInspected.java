package fr.inria.spirals.repairnator;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;

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
}

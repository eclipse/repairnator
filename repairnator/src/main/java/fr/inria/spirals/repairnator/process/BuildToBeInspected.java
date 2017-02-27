package fr.inria.spirals.repairnator.process;

import fr.inria.spirals.jtravis.entities.Build;

/**
 * Created by fernanda on 24/02/17.
 */
public class BuildToBeInspected {

    private Build build;
    private Build previousBuild;
    // For RepairNator, the status is about the build, and for Bears it is about the previous build
    private ScannedBuildStatus status;

    BuildToBeInspected(Build build, ScannedBuildStatus status) {
        this.build = build;
        this.previousBuild = null;
        this.status = status;
    }

    BuildToBeInspected(Build build, Build previousBuild, ScannedBuildStatus status) {
        this.build = build;
        this.previousBuild = previousBuild;
        this.status = status;
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
}

package fr.inria.spirals.repairnator.process;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildStatus;

/**
 * Created by fernanda on 24/02/17.
 */
public class BuildToBeInspected {

    private Build build;
    private Build previousBuild;
    // For RepairNator, the status is about the build, and for Bears it is about the previous build
    private BuildStatus status;

    BuildToBeInspected(Build build, BuildStatus status) {
        this.build = build;
        this.previousBuild = null;
        this.status = status;
    }

    BuildToBeInspected(Build build, Build previousBuild, BuildStatus status) {
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

    public BuildStatus getStatus() {
        return status;
    }
}

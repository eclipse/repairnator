package fr.inria.spirals.jtravis.entities;

import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.jtravis.pojos.RepositoryPojo;

/**
 * Created by urli on 21/12/2016.
 */
public class Repository extends RepositoryPojo {
    private Build lastBuild;

    public Build getLastBuild() {
        if (this.lastBuild == null && this.getLastBuildId() > 0) {
            this.lastBuild = BuildHelper.getBuildFromId(this.getLastBuildId(), this);
        }
        return this.lastBuild;
    }
}

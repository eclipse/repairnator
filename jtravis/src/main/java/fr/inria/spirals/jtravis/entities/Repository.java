package fr.inria.spirals.jtravis.entities;

import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.jtravis.pojos.RepositoryPojo;

/**
 * Business object to deal with repository in Travis CI API
 * A repository can lazily get its last build.
 *
 * @author Simon Urli
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

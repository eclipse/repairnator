package fr.inria.spirals.jtravis.entities;

import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.jtravis.pojos.RepositoryPojo;

import java.util.Date;

/**
 * Business object to deal with repository in Travis CI API
 * A repository can lazily get its last build.
 *
 * @author Simon Urli
 */
public class Repository extends RepositoryPojo {
    private Build lastBuild;
    private Build lastBuildOnMaster;
    private Date lastAccess;

    /**
     * Get the last build of the current repository. If onMaster is specified it will only look for builds created by push on master or cron.
     *
     * @param onMaster
     * @return
     */
    public Build getLastBuild(boolean onMaster) {

        // first case: we should get the last build on master
        if (onMaster) {

            // if we already get the last build and it's not a PR, then it's the last build on master
            if (this.lastBuildOnMaster == null && this.lastBuild != null && !this.lastBuild.isPullRequest()) {
                this.lastBuildOnMaster = this.lastBuild;

            // else we have to request it
            } else if (this.lastBuildOnMaster == null) {
                this.lastBuildOnMaster = BuildHelper.getLastBuildFromMaster(this);
            }
            return this.lastBuildOnMaster;

        // second case: we should get the last build, no matter if it's on master or not
        } else {

            // if we already get the last build on master and it has the same ID as the last build, then there're the same
            if (this.lastBuild == null && this.lastBuildOnMaster != null && this.getLastBuildId() > 0 && this.lastBuildOnMaster.getId() == this.getLastBuildId()) {
                this.lastBuild = this.lastBuildOnMaster;

            // else we have to request it
            } else if (this.lastBuild == null && this.getLastBuildId() > 0) {
                this.lastBuild = BuildHelper.getBuildFromId(this.getLastBuildId(), this);
            }
            return this.lastBuild;
        }
    }

    public Date getLastAccess() {
        return lastAccess;
    }

    public void updateLastAccess() {
        this.lastAccess = new Date();
    }
}

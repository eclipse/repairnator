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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Repository that = (Repository) o;

        return lastBuild != null ? lastBuild.equals(that.lastBuild) : that.lastBuild == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (lastBuild != null ? lastBuild.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Repository{" +
                super.toString() +
                "lastBuild=" + lastBuild +
                '}';
    }
}

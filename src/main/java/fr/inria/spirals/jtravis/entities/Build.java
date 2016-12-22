package fr.inria.spirals.jtravis.entities;

import fr.inria.spirals.jtravis.helpers.RepositoryHelper;
import fr.inria.spirals.jtravis.pojos.BuildPojo;

import java.util.Date;
import java.util.List;

/**
 * Created by urli on 21/12/2016.
 */
public class Build extends BuildPojo {
    private Repository repository;
    private Commit commit;
    private List<Job> jobs;

    public BuildStatus getBuildStatus() {
        if (this.getState() != null) {
            return BuildStatus.valueOf(this.getState().toUpperCase());
        } else {
            return null;
        }
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Repository getRepository() {
        if (repository == null) {
            if (this.getRepositoryId() != 0) {
                this.repository = RepositoryHelper.getRepositoryFromId(this.getRepositoryId());
            }
        }
        return repository;
    }

    public void setCommit(Commit commit) {
        this.commit = commit;
    }

    public Commit getCommit() {
        return commit;
    }

    public List<Job> getJobs() {
        return jobs;
    }

    @Override
    public String toString() {
        return "Build{" +
                super.toString()+
                "commit=" + commit +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Build build = (Build) o;

        return commit != null ? commit.equals(build.commit) : build.commit == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (commit != null ? commit.hashCode() : 0);
        return result;
    }
}

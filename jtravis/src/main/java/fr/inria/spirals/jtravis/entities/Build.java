package fr.inria.spirals.jtravis.entities;

import fr.inria.spirals.jtravis.helpers.JobHelper;
import fr.inria.spirals.jtravis.helpers.RepositoryHelper;
import fr.inria.spirals.jtravis.pojos.BuildPojo;

import java.util.ArrayList;
import java.util.List;

/**
 * Business object to deal with Build of Travis CI API.
 * This object can return its repository, commit, configuration and jobs.
 * It also has helpful methods to get its status and a complete log of its jobs.
 * If the jobs and/or repository are not retrieved when creating the object, there are lazily get on getters.
 *
 * @author Simon Urli
 */
public class Build extends BuildPojo {
    private Repository repository;
    /**
     * If this build came from a PR, defines the repository where the commit is located.
     */
    private Repository prRepository;

    /**
     * In case of a PR, this commit won't be in the tree: it's a merge commit which is created by github and not referenced.
     */
    private Commit commit;

    /**
     * In case of a PR, define the head commit to merge
     */
    private Commit headCommit;

    /**
     * In case of a PR, define the base commit to merge
     */
    private Commit baseCommit;
    private Config config;
    private List<Job> jobs;
    private String completeLog;

    public Build() {
        super();
        this.jobs = new ArrayList<Job>();
    }

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

    public boolean addJob(Job job) {
        if (this.getJobIds().contains(job.getId()) && !jobs.contains(job)) {
            return this.jobs.add(job);
        }
        return false;
    }

    public List<Job> getJobs() {
        if (this.jobs.isEmpty() && !this.getJobIds().isEmpty()) {
            for (int jobId : this.getJobIds()) {
                Job job = JobHelper.getJobFromId(jobId);
                if (job != null) {
                    this.jobs.add(job);
                }
            }
        }
        return jobs;
    }

    public String getCompleteLog() {
        if (!this.getJobs().isEmpty() && (this.completeLog == null || this.completeLog.equals(""))) {
            this.completeLog = "";
            for (Job job : this.getJobs()) {
                Log log = job.getLog();
                if (log != null) {
                    this.completeLog.concat(log.getBody());
                }
            }
        }
        return this.completeLog;
    }

    public Config getConfig() {
        return config;
    }

    public void setConfig(Config config) {
        this.config = config;
    }

    public Repository getPRRepository() {
        return prRepository;
    }

    public void setPRRepository(Repository prRepository) {
        this.prRepository = prRepository;
    }

    public Commit getHeadCommit() {
        return headCommit;
    }

    public void setHeadCommit(Commit headCommit) {
        this.headCommit = headCommit;
    }

    public Commit getBaseCommit() {
        return baseCommit;
    }

    public void setBaseCommit(Commit baseCommit) {
        this.baseCommit = baseCommit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Build build = (Build) o;

        if (repository != null ? !repository.equals(build.repository) : build.repository != null) return false;
        if (prRepository != null ? !prRepository.equals(build.prRepository) : build.prRepository != null) return false;
        if (commit != null ? !commit.equals(build.commit) : build.commit != null) return false;
        if (headCommit != null ? !headCommit.equals(build.headCommit) : build.headCommit != null) return false;
        if (baseCommit != null ? !baseCommit.equals(build.baseCommit) : build.baseCommit != null) return false;
        if (config != null ? !config.equals(build.config) : build.config != null) return false;
        if (jobs != null ? !jobs.equals(build.jobs) : build.jobs != null) return false;
        return completeLog != null ? completeLog.equals(build.completeLog) : build.completeLog == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (repository != null ? repository.hashCode() : 0);
        result = 31 * result + (prRepository != null ? prRepository.hashCode() : 0);
        result = 31 * result + (commit != null ? commit.hashCode() : 0);
        result = 31 * result + (headCommit != null ? headCommit.hashCode() : 0);
        result = 31 * result + (baseCommit != null ? baseCommit.hashCode() : 0);
        result = 31 * result + (config != null ? config.hashCode() : 0);
        result = 31 * result + (jobs != null ? jobs.hashCode() : 0);
        result = 31 * result + (completeLog != null ? completeLog.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        String repo = (repository == null) ? "" : repository.toStringPojo();
        return "Build{" +
                super.toString() +
                ", repository=" + repo +
                ", commit=" + commit +
                ", config=" + config +
                ", jobs=" + jobs +
                ", completeLog='" + completeLog + '\'' +
                ", PRrepository='" + prRepository +'\''+
                ", HeadCommit='" + headCommit +'\'' +
                ", BaseCommit='" + baseCommit +'\'' +
                '}';
    }
}

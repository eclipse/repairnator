package fr.inria.spirals.jtravis.entities;

import fr.inria.spirals.jtravis.helpers.JobHelper;
import fr.inria.spirals.jtravis.helpers.PRInformationHelper;
import fr.inria.spirals.jtravis.helpers.RepositoryHelper;
import fr.inria.spirals.jtravis.parsers.LogParser;
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
public class Build extends BuildPojo implements Comparable<Build> {
    private Repository repository;
    private PRInformation prInformation;
    private Commit commit;
    private Config config;
    private List<Job> jobs;
    private String completeLog;
    private BuildTool buildTool;

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

    public void clearJobs() {
        this.jobs.clear();
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

    public PRInformation getPRInformation() {
        if (isPullRequest() && prInformation == null) {
            prInformation = PRInformationHelper.getPRInformationFromBuild(this);
        }
        return prInformation;
    }

    public BuildTool getBuildTool() {
        if (buildTool == null) {
            if (!this.getJobs().isEmpty()) {
                Job firstJob = this.getJobs().get(0);
                buildTool = firstJob.getBuildTool();
            } else {
                buildTool = BuildTool.UNKNOWN;
            }
        }

        return buildTool;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        Build build = (Build) o;

        if (repository != null ? !repository.equals(build.repository) : build.repository != null) return false;
        if (commit != null ? !commit.equals(build.commit) : build.commit != null) return false;
        if (config != null ? !config.equals(build.config) : build.config != null) return false;
        if (jobs != null ? !jobs.equals(build.jobs) : build.jobs != null) return false;
        return completeLog != null ? completeLog.equals(build.completeLog) : build.completeLog == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (repository != null ? repository.hashCode() : 0);
        result = 31 * result + (commit != null ? commit.hashCode() : 0);
        result = 31 * result + (config != null ? config.hashCode() : 0);
        result = 31 * result + (jobs != null ? jobs.hashCode() : 0);
        result = 31 * result + (completeLog != null ? completeLog.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        String repo = (repository == null) ? "" : repository.toString();
        return "Build{" +
                super.toString() +
                ", repository=" + repo +
                ", commit=" + commit +
                ", config=" + config +
                ", jobs=" + jobs +
                ", completeLog='" + completeLog + '\'' +
                '}';
    }

    @Override
    public int compareTo(Build o) {
        return this.getId()-o.getId();
    }
}

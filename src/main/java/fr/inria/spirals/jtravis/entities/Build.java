package fr.inria.spirals.jtravis.entities;

import java.util.Date;
import java.util.List;

/**
 * Created by urli on 21/12/2016.
 */
public class Build {
    private int id;
    private Repository repository;
    private Commit commit;
    private int number;
    private boolean fromPullRequest;
    private String pullRequestTitle;
    private int pullRequestNumber;
    private BuildStatus status;
    private Date startedAt;
    private Date finishedAt;
    private int duration;
    private List<Job> jobs;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public Commit getCommit() {
        return commit;
    }

    public void setCommit(Commit commit) {
        this.commit = commit;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public boolean isFromPullRequest() {
        return fromPullRequest;
    }

    public void setFromPullRequest(boolean fromPullRequest) {
        this.fromPullRequest = fromPullRequest;
    }

    public String getPullRequestTitle() {
        return pullRequestTitle;
    }

    public void setPullRequestTitle(String pullRequestTitle) {
        this.pullRequestTitle = pullRequestTitle;
    }

    public int getPullRequestNumber() {
        return pullRequestNumber;
    }

    public void setPullRequestNumber(int pullRequestNumber) {
        this.pullRequestNumber = pullRequestNumber;
    }

    public BuildStatus getStatus() {
        return status;
    }

    public void setStatus(BuildStatus status) {
        this.status = status;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public Date getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Date finishedAt) {
        this.finishedAt = finishedAt;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public List<Job> getJobs() {
        return jobs;
    }

    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Build build = (Build) o;

        if (id != build.id) return false;
        if (number != build.number) return false;
        if (fromPullRequest != build.fromPullRequest) return false;
        if (pullRequestNumber != build.pullRequestNumber) return false;
        if (duration != build.duration) return false;
        if (repository != null ? !repository.equals(build.repository) : build.repository != null) return false;
        if (commit != null ? !commit.equals(build.commit) : build.commit != null) return false;
        if (pullRequestTitle != null ? !pullRequestTitle.equals(build.pullRequestTitle) : build.pullRequestTitle != null)
            return false;
        if (status != build.status) return false;
        if (startedAt != null ? !startedAt.equals(build.startedAt) : build.startedAt != null) return false;
        if (finishedAt != null ? !finishedAt.equals(build.finishedAt) : build.finishedAt != null) return false;
        return jobs != null ? jobs.equals(build.jobs) : build.jobs == null;
    }

    @Override
    public int hashCode() {
        return id;
    }
}

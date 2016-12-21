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
}

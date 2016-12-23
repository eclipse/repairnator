package fr.inria.spirals.jtravis.pojos;

import java.util.Date;
import java.util.List;

/**
 * Represent a Build object of Travis CI API (see {@link https://docs.travis-ci.com/api#builds})
 *
 * @author Simon Urli
 */
public class BuildPojo {
    private int id;
    private int repositoryId;
    private int commitId;
    private String number;
    private boolean pullRequest;
    private String pullRequestTitle;
    private int pullRequestNumber;
    private String state;
    private Date startedAt;
    private Date finishedAt;
    private int duration;
    private List<Integer> jobIds;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRepositoryId() {
        return repositoryId;
    }

    public void setRepositoryId(int repositoryId) {
        this.repositoryId = repositoryId;
    }

    public int getCommitId() {
        return commitId;
    }

    public void setCommitId(int commitId) {
        this.commitId = commitId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public boolean isPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(boolean pullRequest) {
        this.pullRequest = pullRequest;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public List<Integer> getJobIds() {
        return jobIds;
    }

    public void setJobIds(List<Integer> jobIds) {
        this.jobIds = jobIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BuildPojo buildPojo = (BuildPojo) o;

        if (id != buildPojo.id) return false;
        if (repositoryId != buildPojo.repositoryId) return false;
        if (commitId != buildPojo.commitId) return false;
        if (pullRequest != buildPojo.pullRequest) return false;
        if (pullRequestNumber != buildPojo.pullRequestNumber) return false;
        if (duration != buildPojo.duration) return false;
        if (number != null ? !number.equals(buildPojo.number) : buildPojo.number != null) return false;
        if (pullRequestTitle != null ? !pullRequestTitle.equals(buildPojo.pullRequestTitle) : buildPojo.pullRequestTitle != null)
            return false;
        if (state != null ? !state.equals(buildPojo.state) : buildPojo.state != null) return false;
        if (startedAt != null ? !startedAt.equals(buildPojo.startedAt) : buildPojo.startedAt != null) return false;
        if (finishedAt != null ? !finishedAt.equals(buildPojo.finishedAt) : buildPojo.finishedAt != null) return false;
        return jobIds != null ? jobIds.equals(buildPojo.jobIds) : buildPojo.jobIds == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + repositoryId;
        result = 31 * result + commitId;
        result = 31 * result + (number != null ? number.hashCode() : 0);
        result = 31 * result + (pullRequest ? 1 : 0);
        result = 31 * result + (pullRequestTitle != null ? pullRequestTitle.hashCode() : 0);
        result = 31 * result + pullRequestNumber;
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (startedAt != null ? startedAt.hashCode() : 0);
        result = 31 * result + (finishedAt != null ? finishedAt.hashCode() : 0);
        result = 31 * result + duration;
        result = 31 * result + (jobIds != null ? jobIds.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BuildPojo{" +
                "id=" + id +
                ", repositoryId=" + repositoryId +
                ", commitId=" + commitId +
                ", number='" + number + '\'' +
                ", pullRequest=" + pullRequest +
                ", pullRequestTitle='" + pullRequestTitle + '\'' +
                ", pullRequestNumber=" + pullRequestNumber +
                ", state='" + state + '\'' +
                ", startedAt=" + startedAt +
                ", finishedAt=" + finishedAt +
                ", duration=" + duration +
                ", jobIds=" + jobIds +
                '}';
    }
}

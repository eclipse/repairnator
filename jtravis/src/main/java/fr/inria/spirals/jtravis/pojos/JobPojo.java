package fr.inria.spirals.jtravis.pojos;

import java.util.Date;

/**
 * Represents a job object in Travis CI API (see {@link https://docs.travis-ci.com/api#jobs})
 *
 * @author Simon Urli
 */
public class JobPojo {
    private int id;
    private int repositoryId;
    private int buildId;
    private int commitId;
    private int logId;
    private String state;
    private String number;
    private Date startedAt;
    private Date finishedAt;
    private String queue;
    private boolean allowFailure;

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

    public int getBuildId() {
        return buildId;
    }

    public void setBuildId(int buildId) {
        this.buildId = buildId;
    }

    public int getCommitId() {
        return commitId;
    }

    public void setCommitId(int commitId) {
        this.commitId = commitId;
    }

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
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

    public String getQueue() {
        return queue;
    }

    public void setQueue(String queue) {
        this.queue = queue;
    }

    public boolean isAllowFailure() {
        return allowFailure;
    }

    public void setAllowFailure(boolean allowFailure) {
        this.allowFailure = allowFailure;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JobPojo jobPojo = (JobPojo) o;

        if (id != jobPojo.id) return false;
        if (repositoryId != jobPojo.repositoryId) return false;
        if (buildId != jobPojo.buildId) return false;
        if (commitId != jobPojo.commitId) return false;
        if (logId != jobPojo.logId) return false;
        if (allowFailure != jobPojo.allowFailure) return false;
        if (state != null ? !state.equals(jobPojo.state) : jobPojo.state != null) return false;
        if (number != null ? !number.equals(jobPojo.number) : jobPojo.number != null) return false;
        if (startedAt != null ? !startedAt.equals(jobPojo.startedAt) : jobPojo.startedAt != null) return false;
        if (finishedAt != null ? !finishedAt.equals(jobPojo.finishedAt) : jobPojo.finishedAt != null) return false;
        return queue != null ? queue.equals(jobPojo.queue) : jobPojo.queue == null;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + repositoryId;
        result = 31 * result + buildId;
        result = 31 * result + commitId;
        result = 31 * result + logId;
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (number != null ? number.hashCode() : 0);
        result = 31 * result + (startedAt != null ? startedAt.hashCode() : 0);
        result = 31 * result + (finishedAt != null ? finishedAt.hashCode() : 0);
        result = 31 * result + (queue != null ? queue.hashCode() : 0);
        result = 31 * result + (allowFailure ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "JobPojo{" +
                "id=" + id +
                ", repositoryId=" + repositoryId +
                ", buildId=" + buildId +
                ", commitId=" + commitId +
                ", logId=" + logId +
                ", state='" + state + '\'' +
                ", number='" + number + '\'' +
                ", startedAt=" + startedAt +
                ", finishedAt=" + finishedAt +
                ", queue='" + queue + '\'' +
                ", allowFailure=" + allowFailure +
                '}';
    }
}

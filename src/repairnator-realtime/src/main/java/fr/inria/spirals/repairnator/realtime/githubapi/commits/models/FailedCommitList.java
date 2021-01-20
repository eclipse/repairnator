package fr.inria.spirals.repairnator.realtime.githubapi.commits.models;

import java.util.List;

public class FailedCommitList {
    private List<FailedCommit> failedCommits;
    private Integer totalCount;

    public FailedCommitList(List<FailedCommit> failedCommits){
        this.failedCommits = failedCommits;
        this.totalCount = failedCommits.size();
    }

    public List<FailedCommit> getFailedCommits() {
        return failedCommits;
    }

    public void setFailedCommits(List<FailedCommit> failedCommits) {
        this.failedCommits = failedCommits;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
}

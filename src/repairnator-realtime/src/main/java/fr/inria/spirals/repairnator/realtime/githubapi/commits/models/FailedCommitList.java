package fr.inria.spirals.repairnator.realtime.githubapi.commits.models;

import java.util.List;

public class FailedCommitList {
    private List<SelectedCommit> failedCommits;
    private Integer totalCount;

    public FailedCommitList(List<SelectedCommit> failedCommits){
        this.failedCommits = failedCommits;
        this.totalCount = failedCommits.size();
    }

    public List<SelectedCommit> getFailedCommits() {
        return failedCommits;
    }

    public void setFailedCommits(List<SelectedCommit> failedCommits) {
        this.failedCommits = failedCommits;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
}

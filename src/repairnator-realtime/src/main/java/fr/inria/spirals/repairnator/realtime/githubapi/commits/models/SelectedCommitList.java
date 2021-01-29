package fr.inria.spirals.repairnator.realtime.githubapi.commits.models;

import java.util.List;

public class SelectedCommitList {
    private List<SelectedCommit> selectedCommits;
    private Integer totalCount;

    public SelectedCommitList(List<SelectedCommit> selectedCommits){
        this.selectedCommits = selectedCommits;
        this.totalCount = selectedCommits.size();
    }

    public List<SelectedCommit> getSelectedCommits() {
        return selectedCommits;
    }

    public void setSelectedCommits(List<SelectedCommit> selectedCommits) {
        this.selectedCommits = selectedCommits;
    }

    public Integer getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Integer totalCount) {
        this.totalCount = totalCount;
    }
}

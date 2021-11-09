package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.SelectedPullRequest;
import fr.inria.spirals.repairnator.realtime.githubapi.pullrequests.GithubAPIPullRequestAdapter;

import java.util.List;
import java.util.Set;

public class GithubPullRequestScanner {

    FetchMode fetchMode;
    Set<String> repos;

    public GithubPullRequestScanner(GithubPullRequestScanner.FetchMode fetchMode, Set<String> repos) {
        this.fetchMode = fetchMode;
        this.repos = repos;
    }

    public List<SelectedPullRequest> fetch(long startDateForScanning, long startTime, String repo, boolean isFirstScan) throws Exception {
        return GithubAPIPullRequestAdapter.getInstance().getSelectedPullRequests(startDateForScanning, startTime, isFirstScan, fetchMode, repo);
    }

    public enum FetchMode {
        FAILED, ALL, PASSING
    }
}

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

    public List<SelectedPullRequest> fetch(long startTime, String repo) throws Exception {
        return GithubAPIPullRequestAdapter.getInstance().getSelectedPullRequests(startTime, fetchMode, repo);
    }

    public enum FetchMode {
        FAILED, ALL, PASSING
    }
}

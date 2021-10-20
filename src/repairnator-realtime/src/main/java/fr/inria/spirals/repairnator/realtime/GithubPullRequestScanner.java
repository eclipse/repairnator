package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.realtime.githubapi.pullrequests.GithubAPIPullRequestAdapter;

import java.util.List;

public class GithubPullRequestScanner {

    static long scanIntervalDelay = 60 * 15; // 15 min
    static long lastScanInterval = 0L;

    public List<SelectedPullRequest> fetch() throws Exception {
        long endTime = System.currentTimeMillis() - scanIntervalDelay;
        long startTime = lastScanInterval;
        lastScanInterval = endTime;

        return fetch(startTime, endTime);
    }

    public List<SelectedPullRequest> fetch(long startTime, long endTime) throws Exception {
        return GithubAPIPullRequestAdapter.getInstance().getSelectedPullRequests(startTime, endTime, fetchMode, repos);
    }

}

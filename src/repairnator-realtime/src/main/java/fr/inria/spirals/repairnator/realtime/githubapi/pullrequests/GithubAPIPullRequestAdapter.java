package fr.inria.spirals.repairnator.realtime.githubapi.pullrequests;

import fr.inria.spirals.repairnator.realtime.GithubScanner;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.SelectedCommit;
import org.kohsuke.github.GHCommitQueryBuilder;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GithubAPIPullRequestAdapter {

    public List<SelectedPullRequest> getSelectedPullRequests
            (
                    GHRepository repo,
                    long since,
                    long until,
                    GithubScanner.FetchMode fetchMode
            ) throws IOException {
        List<SelectedCommit> res = new ArrayList<>();

        GHCommitQueryBuilder query = repo.queryPullRequests();

        // Filter PR that are OPEN and FAILING

        // Check if it has new commits since some timestamp

        // SelectedPullRequests
        return null;
    }

}

package fr.inria.spirals.repairnator.realtime.githubapi.pullrequests;

import fr.inria.spirals.repairnator.realtime.GithubPullRequestScanner;
import fr.inria.spirals.repairnator.realtime.githubapi.GAA;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.SelectedPullRequest;
import org.kohsuke.github.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GithubAPIPullRequestAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GithubAPIPullRequestAdapter.class);

    private static GithubAPIPullRequestAdapter _instance;

    public static GithubAPIPullRequestAdapter getInstance() {
        if (_instance == null) {
            _instance = new GithubAPIPullRequestAdapter();
        }
        return _instance;
    }

    public List<SelectedPullRequest> getSelectedPullRequests
            (
                    GHRepository repo,
                    long since,
                    GithubPullRequestScanner.FetchMode fetchMode
            ) throws IOException {

        List<SelectedPullRequest> res = new ArrayList<>();

        // Search for all Pull Requests that are open
        GHPullRequestQueryBuilder query = repo.queryPullRequests().state(GHIssueState.OPEN);

        List<GHPullRequest> pullRequests;

        if (since == 0) { // Only at the first execution
            pullRequests = query.list().toList();
        } else {
            pullRequests = query.list().toList().stream().filter(pr -> {
                try {
                    List<GHIssueComment> comments = pr.getComments();

                    /*
                     * It checks if the last update of a pull request is happened after "since" parameter value.
                     * To avoid considering updates that are related to the addition of comments to the
                     * pull request, it checks if the date of the last comment is different from the
                     * date of the last update. This is a sign that the last update of the pull request is
                     * not related to the addition of a comment to the pull request.
                     */
                    if (comments != null && comments.size() > 0) {
                        return pr.getUpdatedAt().getTime() >= since &&
                                comments.get(comments.size() - 1).getUpdatedAt().compareTo(pr.getUpdatedAt()) != 0;
                    } else {
                        return pr.getUpdatedAt().getTime() >= since;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                return false;
            }).collect(Collectors.toList());
        }

        for (GHPullRequest pullRequest : pullRequests) {
            boolean isGithubPullRequestFailed = false;
            // It checks for pull requests whose head commit has a failure
            try {
                PagedIterable<GHCheckRun> checkRuns = pullRequest.getHead().getCommit().getCheckRuns();
                for (GHCheckRun check : checkRuns) {
                    GHCheckRun.Conclusion conclusion = check.getConclusion();
                    if (conclusion != null && conclusion.equals(GHCheckRun.Conclusion.FAILURE)) {
                        isGithubPullRequestFailed = true;
                        break;
                    }
                }

                switch (fetchMode) {
                    case ALL:
                        res.add(new SelectedPullRequest(pullRequest.getId(),
                                pullRequest.getNumber(), pullRequest.getUrl().toString(),
                                pullRequest.getHead().getCommit().getSHA1(), pullRequest.getRepository().getFullName()));
                        break;
                    case FAILED:
                        if (isGithubPullRequestFailed) {
                            res.add(new SelectedPullRequest(pullRequest.getId(),
                                    pullRequest.getNumber(), pullRequest.getUrl().toString(),
                                    pullRequest.getHead().getCommit().getSHA1(), pullRequest.getRepository().getFullName()));
                        }
                        break;
                }
            } catch (NullPointerException npe) {
                LOGGER.error("ERROR | Kohsuke GitHub Library got an error while retrieving the head commit of pull request " + pullRequest.getUrl().toString());
            }
        }
        return res;
    }

    public List<SelectedPullRequest> getSelectedPullRequests
            (
                    long intervalStart,
                    GithubPullRequestScanner.FetchMode fetchMode,
                    Set<String> fixedRepos
            ) {

        List<SelectedPullRequest> selectedPullRequests = Collections.synchronizedList(new ArrayList<>());
        fixedRepos.parallelStream().forEach(repoName -> {
            try {
                GHRepository repo = GAA.g().getRepository(repoName);

                boolean isMaven = false;
                for (GHTreeEntry treeEntry : repo.getTree("HEAD").getTree()) {
                    if (treeEntry.getPath().equals("pom.xml")) {
                        isMaven = true;
                        break;
                    }
                }

                if (!isMaven) {
                    return;
                }

                selectedPullRequests.addAll(GithubAPIPullRequestAdapter.getInstance()
                        .getSelectedPullRequests(repo, intervalStart, fetchMode));

            } catch (Exception e) {
                System.err.println("error occurred for: " + repoName);
                e.printStackTrace();
            }
        });
        return selectedPullRequests;
    }
}

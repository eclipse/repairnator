package fr.inria.spirals.repairnator.realtime.githubapi.pullrequests;

import fr.inria.spirals.repairnator.realtime.GithubPullRequestScanner;
import fr.inria.spirals.repairnator.realtime.githubapi.GAA;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.SelectedPullRequest;
import org.kohsuke.github.*;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GithubAPIPullRequestAdapter {

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

        List<GHPullRequest> pullRequestsToAnalyze;

        if (since == 0) { // Only at the first execution
            pullRequestsToAnalyze = query.list().toList();
        } else {
            List<GHPullRequest> allPullRequests = query.list().toList();
            pullRequestsToAnalyze = new ArrayList<>();

            allPullRequests.forEach(pullRequest -> {
                try {
                    List<GHIssueComment> comments = pullRequest.getComments();
                    List<GHPullRequestReviewComment> pullRequestReviewComments = pullRequest.listReviewComments().toList();

                    boolean isUpdateToConsider = true;

                    if (pullRequest.getUpdatedAt().getTime() >= since) {

                        // Avoid considering updates that are related to the addition of a new comment
                        if (!comments.isEmpty()) {
                            if (comments.get(comments.size() - 1).getUpdatedAt().compareTo(pullRequest.getUpdatedAt()) == 0) {
                                isUpdateToConsider = false;
                            }
                        }
                        // Avoid considering updates that are related to the addition of a new review comment
                        if (isUpdateToConsider && !pullRequestReviewComments.isEmpty()) {
                            if (pullRequestReviewComments.get(pullRequestReviewComments.size() - 1).getUpdatedAt().compareTo(pullRequest.getUpdatedAt()) == 0) {
                                isUpdateToConsider = false;
                            }
                        }

                        if (isUpdateToConsider) {
                            pullRequestsToAnalyze.add(pullRequest);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        for (GHPullRequest pullRequest : pullRequestsToAnalyze) {

            boolean isGithubPullRequestFailed = false;

            // It checks for pull requests whose head commit has a failure
            List<GHPullRequestCommitDetail> commits = pullRequest.listCommits().toList();
            String headCommitSHA = commits.get(commits.size()-1).getSha();
            List<GHCommitStatus> statuses = repo.getCommit(headCommitSHA).listStatuses().toList();

            List<GHCheckRun> checkRuns = repo.getCommit(headCommitSHA).getCheckRuns().toList();

            // Check if a pull request has some failed checks
            for (int i = 0; i < checkRuns.size(); i++) {
                if (checkRuns.get(i) != null && checkRuns.get(i).getConclusion() != null && checkRuns.get(i).getConclusion().name().equalsIgnoreCase("FAILURE")) {
                    isGithubPullRequestFailed = true;
                    break;
                }
            }

            // Another check using the status of a commit instead of the check runs
            if (!isGithubPullRequestFailed) {
                for (int i = 0; i < statuses.size(); i++) {
                    if (statuses.get(i) != null && statuses.get(i).getState() != null && statuses.get(i).getState().name().equalsIgnoreCase("failure")) {
                        isGithubPullRequestFailed = true;
                        break;
                    }
                }
            }

            switch (fetchMode) {
                case ALL:
                    res.add(new SelectedPullRequest(pullRequest.getId(),
                            pullRequest.getNumber(), pullRequest.getUrl().toString(),
                            headCommitSHA, pullRequest.getRepository().getFullName()));
                    break;
                case FAILED:
                    if (isGithubPullRequestFailed) {
                        res.add(new SelectedPullRequest(pullRequest.getId(),
                                pullRequest.getNumber(), pullRequest.getUrl().toString(),
                                headCommitSHA, pullRequest.getRepository().getFullName()));
                    }
                    break;
            }
        }
        return res;
    }

    public List<SelectedPullRequest> getSelectedPullRequests
            (
                    long intervalStart,
                    GithubPullRequestScanner.FetchMode fetchMode,
                    String fixedRepos
            ) {

        List<SelectedPullRequest> selectedPullRequests = Collections.synchronizedList(new ArrayList<>());

            try {
                GHRepository repo = GAA.g().getRepository(fixedRepos);

                boolean isMaven = false;
                for (GHTreeEntry treeEntry : repo.getTree("HEAD").getTree()) {
                    if (treeEntry.getPath().equals("pom.xml")) {
                        isMaven = true;
                        break;
                    }
                }

                if (!isMaven) {
                    return null;
                }

                selectedPullRequests.addAll(GithubAPIPullRequestAdapter.getInstance()
                        .getSelectedPullRequests(repo, intervalStart, fetchMode));

            } catch (Exception e) {
                System.err.println("error occurred for: " + fixedRepos);
                e.printStackTrace();
            }

        return selectedPullRequests;
    }
}

package fr.inria.spirals.repairnator.scanner;

import fr.inria.spirals.repairnator.realtime.GithubPullRequestScanner;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.SelectedPullRequest;
import org.junit.Test;

import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class GithubPullRequestScannerTest {

    // Test to detect failing open pull requests
    @Test
    public void testFetchFailingPullRequests() throws Exception {
        Set<String> repositorySet = new HashSet<>();
        String repository = "repairnator/failingProject";
        repositorySet.add(repository);

        GithubPullRequestScanner scanner =
                new GithubPullRequestScanner(GithubPullRequestScanner.FetchMode.FAILED, repositorySet);

        // It searches for all the failing open pull requests opened after the current instant
        List<SelectedPullRequest> selectedPullRequestList =
                scanner.fetch(Instant.now().toEpochMilli(), System.currentTimeMillis(), repository, true);

        assertEquals(0, selectedPullRequestList.size());

        // It searches for all the failing open pull requests opened after Jan 01 01:00:00 CET 1970
        selectedPullRequestList = scanner.fetch(new Date(0).getTime(), System.currentTimeMillis(), repository, true);
        assertTrue(selectedPullRequestList.size() > 0);

        // https://github.com/repairnator/failingProject/pull/7 (failing pull request)
        assertTrue(selectedPullRequestList.stream().anyMatch(pr -> pr.getNumber() == 7));

        // https://github.com/repairnator/failingProject/pull/3 (not failing pull request)
        assertFalse(selectedPullRequestList.stream().anyMatch(pr -> pr.getNumber() == 3));
    }

    // Test to detect successful and failing open pull requests
    @Test
    public void testFetchAllOpenPullRequests() throws Exception {
        Set<String> repositorySet = new HashSet<>();
        String repository = "repairnator/failingProject";
        repositorySet.add(repository);

        GithubPullRequestScanner scanner =
                new GithubPullRequestScanner(GithubPullRequestScanner.FetchMode.ALL, repositorySet);

        // It searches for all open pull requests opened after Jan 01 01:00:00 CET 1970
        List<SelectedPullRequest> selectedPullRequestList = selectedPullRequestList = scanner.fetch(new Date(0).getTime(), System.currentTimeMillis(), repository, true);
        assertTrue(selectedPullRequestList.size() > 0);

        // https://github.com/repairnator/failingProject/pull/3 (not failing pull request)
        assertTrue(selectedPullRequestList.stream().anyMatch(pr -> pr.getNumber() == 3));

        // https://github.com/repairnator/failingProject/pull/7 (failing pull request)
        assertTrue(selectedPullRequestList.stream().anyMatch(pr -> pr.getNumber() == 7));
    }
}

package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.InputBuild;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.SelectedCommit;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import static junit.framework.TestCase.*;
import static org.mockito.Mockito.*;

import org.mockito.internal.util.reflection.Whitebox;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TestGithubScanner {


    @Test
    public void testBuildSubmission(){
        DockerPipelineRunner runner = mock(DockerPipelineRunner.class);

        GithubScanner scanner = new GithubScanner();
        Whitebox.setInternalState(scanner, "runner", runner);

        boolean isGithubActionsFailed = false;
        String commitId = "fda5d6161a5602a76e810209491d04cf91f4803b";
        String repoName = "repairnator/failingProject";
        SelectedCommit commit = new SelectedCommit(isGithubActionsFailed, commitId, repoName);

        scanner.process(commit);
        verify(runner, times(1)).submitBuild(any(InputBuild.class));
    }

    @Test
    public void testFetchingAll() throws Exception {
        // repo https://github.com/castor-software/depclean/commits/master
        Set<String> repos = new HashSet<String>(FileUtils.readLines(new File(getClass()
                        .getResource("/GithubScannerTest_repos.txt").getFile()), "UTF-8"));
        GithubScanner scanner = new GithubScanner();
        scanner.setup();

        List<SelectedCommit> commits =
                scanner.fetch(new SimpleDateFormat("dd/MM/yyyy").parse("11/06/2022").getTime(),
                        new SimpleDateFormat("dd/MM/yyyy").parse("14/12/2022").getTime(),
                        GithubScanner.FetchMode.ALL, repos);
        assertTrue(commits.stream().anyMatch(x -> x.getCommitId().equals("2693a1e2da8d74135c7c9be1e1aa5758c30f86f7")));
        assertTrue(commits.stream().anyMatch(x -> x.getCommitId().equals("4460f47ef5f7c482efc9b953312e79ebfc0eebdd")));
        assertTrue(commits.stream().anyMatch(x -> x.getCommitId().equals("36b10489c6ae71fba089c8db551d0ce2663d19d4")));
        assertTrue(commits.stream().anyMatch(x -> x.getCommitId().equals("7411d15444a88cd5832364f3434ea66779de5918")));

    }

    @Test
    public void testFetchingFailed() throws Exception {
        // repo https://github.com/castor-software/depclean/commits/master
        Set<String> repos = new HashSet<String>(FileUtils.readLines(new File(getClass()
                .getResource("/GithubScannerTest_repos.txt").getFile()), "UTF-8"));
        GithubScanner scanner = new GithubScanner();
        scanner.setup();

        List<SelectedCommit> commits =
                scanner.fetch(new SimpleDateFormat("dd/MM/yyyy").parse("11/06/2022").getTime(),
                        new SimpleDateFormat("dd/MM/yyyy").parse("14/12/2022").getTime(),
                        GithubScanner.FetchMode.FAILED, repos);
        assertFalse(commits.stream().anyMatch(x -> x.getCommitId().equals("2693a1e2da8d74135c7c9be1e1aa5758c30f86f7")));
        assertTrue(commits.stream().anyMatch(x -> x.getCommitId().equals("4460f47ef5f7c482efc9b953312e79ebfc0eebdd")));
    }

    @Test
    public void testFetchingPassing() throws Exception {
        // repo https://github.com/castor-software/depclean/commits/master
        Set<String> repos = new HashSet<String>(FileUtils.readLines(new File(getClass()
                .getResource("/GithubScannerTest_repos.txt").getFile()), "UTF-8"));
        GithubScanner scanner = new GithubScanner();
        scanner.setup();

        List<SelectedCommit> commits =
                scanner.fetch(new SimpleDateFormat("dd/MM/yyyy").parse("11/06/2022").getTime(),
                        new SimpleDateFormat("dd/MM/yyyy").parse("14/12/2022").getTime(),
                        GithubScanner.FetchMode.PASSING, repos);
        assertTrue(commits.stream().anyMatch(x -> x.getCommitId().equals("36b10489c6ae71fba089c8db551d0ce2663d19d4")));
        assertFalse(commits.stream().anyMatch(x -> x.getCommitId().equals("7411d15444a88cd5832364f3434ea66779de5918")));
    }

}

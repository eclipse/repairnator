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
                scanner.fetch(new SimpleDateFormat("dd/MM/yyyy").parse("14/12/2022").getTime(),
                        new SimpleDateFormat("dd/MM/yyyy").parse("10/04/2023").getTime(),
                        GithubScanner.FetchMode.ALL, repos);
        assertTrue(commits.stream().anyMatch(x -> x.getCommitId().equals("462bca1472db3198c2e47aff072126209f796bce")));
        assertTrue(commits.stream().anyMatch(x -> x.getCommitId().equals("f8a2607178ed48e5843768c3cbc8e60406d63a66")));
        assertTrue(commits.stream().anyMatch(x -> x.getCommitId().equals("e2e8afe4a06c437c5eb4a3edc20c3152450ac702")));
        assertTrue(commits.stream().anyMatch(x -> x.getCommitId().equals("5180598adbdff8f9d3c53c6e647290614bf752b9")));

    }

    @Test
    public void testFetchingFailed() throws Exception {
        // repo https://github.com/castor-software/depclean/commits/master
        // for future mantainer make  sure the failing commit is not related to a codecov failure
        Set<String> repos = new HashSet<String>(FileUtils.readLines(new File(getClass()
                .getResource("/GithubScannerTest_repos.txt").getFile()), "UTF-8"));
        GithubScanner scanner = new GithubScanner();
        scanner.setup();

        List<SelectedCommit> commits =
                scanner.fetch(new SimpleDateFormat("dd/MM/yyyy").parse("14/06/2021").getTime(),
                        new SimpleDateFormat("dd/MM/yyyy").parse("10/09/2022").getTime(),
                        GithubScanner.FetchMode.FAILED, repos);

        assertFalse(commits.stream().anyMatch(x -> x.getCommitId().equals("063572c0b7747498940a06c240a71193ec8314ee")));
        assertTrue(commits.stream().anyMatch(x -> x.getCommitId().equals("332672710a7a90c5c51a09064f0917435bd5a6ec")));
    }

    @Test
    public void testFetchingPassing() throws Exception {
        // repo https://github.com/castor-software/depclean/commits/master
        Set<String> repos = new HashSet<String>(FileUtils.readLines(new File(getClass()
                .getResource("/GithubScannerTest_repos.txt").getFile()), "UTF-8"));
        GithubScanner scanner = new GithubScanner();
        scanner.setup();

        List<SelectedCommit> commits =
                scanner.fetch(new SimpleDateFormat("dd/MM/yyyy").parse("14/06/2021").getTime(),
                        new SimpleDateFormat("dd/MM/yyyy").parse("10/09/2021").getTime(),
                        GithubScanner.FetchMode.PASSING, repos);
        assertTrue(commits.stream().anyMatch(x -> x.getCommitId().equals("08cddb1c57eebb3ccd320061b75fd26cb09fc1fd")));
        assertFalse(commits.stream().anyMatch(x -> x.getCommitId().equals("332672710a7a90c5c51a09064f0917435bd5a6ec")));
    }

}

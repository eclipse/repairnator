package fr.inria.spirals.repairnator.realtime;

import fr.inria.spirals.repairnator.InputBuild;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.FailedCommit;
import org.junit.Test;
import static org.mockito.Mockito.*;

import org.mockito.internal.util.reflection.Whitebox;

public class TestGithubScanner {


    @Test
    public void TestBuildSubmission(){
        DockerPipelineRunner runner = mock(DockerPipelineRunner.class);

        GithubScanner scanner = new GithubScanner();
        Whitebox.setInternalState(scanner, "runner", runner);

        boolean isTravisFailed = true;
        boolean isGithubActionsFailed = false;
        String commitId = "65eb0ee8cc221bd4fe6d6414feb6ee368131288d";
        String repoName = "javierron/failingProject";
        FailedCommit commit = new FailedCommit(isTravisFailed, isGithubActionsFailed, commitId, repoName);

        scanner.process(commit);
        verify(runner, times(1)).submitBuild(any(InputBuild.class));
    }

}

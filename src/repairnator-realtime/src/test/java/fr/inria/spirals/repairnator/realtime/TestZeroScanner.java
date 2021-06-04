package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.Commit;
import fr.inria.jtravis.entities.v2.BuildV2;
import fr.inria.spirals.repairnator.InputBuild;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.realtime.githubapi.commits.models.SelectedCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class TestZeroScanner {

    @Mock
    DockerPipelineRunner runner;
    @Mock
    SequencerCollector collector;

    @Spy
    @InjectMocks
    ZeroScanner scanner = new ZeroScanner();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ZeroScanner.setup();
    }

    @After
    public void cleanup(){
        //config singleton is not reset between tests and
        //this setting causes some interference
        RepairnatorConfig.getInstance().setDockerImageName(null);
    }

    @Test
    public void TestAttemptJob () {
        SelectedCommit commit = new SelectedCommit(true, "javierron/failingProject", "65eb0ee8cc221bd4fe6d6414feb6ee368131288d");
        scanner.attemptJob(commit); //failing job
        verify(runner, times(1)).submitBuild(any(InputBuild.class));

    }

    @Test
    public void TestCollectJob () {

        SelectedCommit commit = new SelectedCommit(false, "javierron/failingProject", "bc7c358653159be5caece027258b822e47dc894c");
        scanner.collectJob(commit); //passing job
        verify(collector, times(1)).handle(anyString(), anyString());
    }

}

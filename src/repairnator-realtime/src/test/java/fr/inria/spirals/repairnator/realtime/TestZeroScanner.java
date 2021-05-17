package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.entities.Commit;
import fr.inria.jtravis.entities.v2.BuildV2;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
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

    Build build;
    BuildV2 buildV2;

    @Mock
    BuildHelperV2 buildHelper;
    @Mock
    RTScanner rtScanner;
    @Mock
    SequencerCollector collector;

    @Spy
    @InjectMocks
    ZeroScanner scanner = new ZeroScanner();

    @Before
    public void setup() {
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setJTravisEndpoint("https://api.travis-ci.com");

        build = new Build();
        buildV2 = new BuildV2();
        buildV2.setCommit(new Commit());

        MockitoAnnotations.initMocks(this);

        when(buildHelper.fromId(anyLong())).thenReturn(Optional.of(build));
        when(buildHelper.fromIdV2(anyLong())).thenReturn(Optional.of(buildV2));

        scanner.setup();
    }

    @After
    public void cleanup(){
        //config singleton is not reset between tests and
        //this setting causes some interference
        RepairnatorConfig.getInstance().setDockerImageName(null);
    }

    @Test
    public void TestAttemptJob () {
        scanner.attemptJob(224246334); // failing job - https://travis-ci.com/github/repairnator/failingProject/builds/224246334
        verify(rtScanner, times(1)).submitBuildToExecution(any(Build.class));
    }

    @Test
    public void TestCollectJob () {
        scanner.collectJob(220482792, "repairnator/failingProject"); // passing job - https://travis-ci.com/github/repairnator/failingProject/builds/220482792
        verify(collector, times(1)).handle(anyString(), anyString());
    }

}

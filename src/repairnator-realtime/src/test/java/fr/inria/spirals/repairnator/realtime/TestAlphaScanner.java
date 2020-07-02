package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.Build;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class TestAlphaScanner {

    @Mock
    RTScanner rtScanner;
    @Mock
    SequencerCollector collector;

    @Spy
    @InjectMocks
    AlphaScanner scanner = new AlphaScanner();

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        scanner.setup();
    }

    @Test
    public void TestAttemptJob () {
        scanner.attemptJob(702053045); //failing job
        verify(rtScanner, times(1)).submitBuildToExecution(any(Build.class));
    }

    @Test
    public void TestCollectJob () {
        scanner.collectJob(704352008, "javierron/failingProject"); //passing job
        verify(collector, times(1)).handle(anyString(), anyString());
    }

}

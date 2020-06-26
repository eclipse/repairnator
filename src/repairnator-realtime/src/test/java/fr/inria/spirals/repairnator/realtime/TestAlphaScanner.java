package fr.inria.spirals.repairnator.realtime;

import fr.inria.jtravis.entities.v2.JobV2;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TestAlphaScanner {

    @Test
    public void TestAttemptJob () {
        AlphaScanner scanner = new AlphaScanner();

        scanner.setup();
        scanner.attemptJob(702053045);

        // meaningful assertions
    }

}

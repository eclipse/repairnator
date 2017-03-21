package fr.inria.spirals.jtravis.helpers;

import fr.inria.spirals.jtravis.entities.Job;
import fr.inria.spirals.jtravis.entities.Log;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Created by urli on 23/12/2016.
 */
public class LogHelperTest {
    static String logBegin = "travis_fold:start:worker_info";

    static String passingLogEnd = "Done. Your build exited with 0.";
    static String failingLogEnd = "Done. Your build exited with 1.";
    static String erroringLogEnd = "Your build has been stopped.";

    @Test
    public void testGetLogFromJobOtherThanPassingOrFailingReturnNull() {
        Job job = new Job();
        job.setId(185719844);
        job.setState("started");

        Log obtainedLog = LogHelper.getLogFromJob(job);
        assertTrue(obtainedLog == null);
    }

    @Test
    public void testGetPassedLogFromId() {
        Job job = new Job();
        job.setId(185719844);
        job.setState("passed");

        int length = 1499916;

        Log obtainedLog = LogHelper.getLogFromJob(job);
        assertEquals(185719844, obtainedLog.getJobId());
        assertTrue(obtainedLog.getBody() != null);
        assertEquals(length, obtainedLog.getBody().length());
        assertTrue(obtainedLog.getBody().trim().startsWith(logBegin));
        assertTrue(obtainedLog.getBody().trim().endsWith(passingLogEnd));
    }

    @Test
    public void testGetFailingLogFromId() {
        int jobId = 212676600;
        Job job = new Job();
        job.setId(jobId);
        job.setState("failed");

        int length = 163775;

        Log obtainedLog = LogHelper.getLogFromJob(job);
        assertEquals(jobId, obtainedLog.getJobId());
        assertTrue(obtainedLog.getBody() != null);
        assertEquals(length, obtainedLog.getBody().length());
        assertTrue(obtainedLog.getBody().trim().startsWith(logBegin));
        assertTrue(obtainedLog.getBody().trim().endsWith(failingLogEnd));
    }

    @Test
    public void testGetErroringLogFromId() {
        int jobId = 210935305;
        Job job = new Job();
        job.setId(jobId);
        job.setState("errored");

        int length = 91188;

        Log obtainedLog = LogHelper.getLogFromJob(job);
        assertEquals(jobId, obtainedLog.getJobId());
        assertTrue(obtainedLog.getBody() != null);
        assertEquals(length, obtainedLog.getBody().length());
        assertTrue(obtainedLog.getBody().trim().startsWith(logBegin));
        assertTrue(obtainedLog.getBody().trim().endsWith(erroringLogEnd));
    }
}

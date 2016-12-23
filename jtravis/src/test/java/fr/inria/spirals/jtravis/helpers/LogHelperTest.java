package fr.inria.spirals.jtravis.helpers;

import fr.inria.spirals.jtravis.entities.Log;
import org.junit.Test;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

/**
 * Created by urli on 23/12/2016.
 */
public class LogHelperTest {

    @Test
    public void testGetLogFromId() {
        Log expectedLog = new Log();
        expectedLog.setId(135819715);
        expectedLog.setJobId(185719844);
        expectedLog.setType("Log");
        expectedLog.setBody("");

        Log obtainedLog = LogHelper.getLogFromId(135819715);
        assertEquals(expectedLog, obtainedLog);
    }

    @Test
    public void testGetBodyFromArchivedLog() {
        String begin = "travis_fold:start:worker_info";

        String end = "Done. Your build exited with 0.";

        int length = 1499914;

        Log obtainedLog = LogHelper.getLogFromId(135819715);
        String body = obtainedLog.getBody();
        body = body.trim();

        assertTrue(body != null);
        assertEquals(length, body.length());
        assertTrue(body.startsWith(begin));
        assertTrue(body.endsWith(end));
    }
}

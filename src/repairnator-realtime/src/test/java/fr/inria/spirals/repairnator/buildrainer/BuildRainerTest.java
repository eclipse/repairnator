package fr.inria.spirals.repairnator.buildrainer;

import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;

public class BuildRainerTest {

    @Test
    /**
     * This test if BuildRainer can connect
     * and communicate with a ws server
     * Remove ignore to test, this can be a bit flaky.
     */
    public void testWebSocket()
    {
        TestServer testServer = TestServer.getInstance();
        testServer.setReuseAddr(true);
        testServer.run();
        String receivedMsg = testServer.getBuildRainer().getRecentMessage();

        assertEquals(receivedMsg,"Test");
    }
}

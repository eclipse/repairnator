package fr.inria.repairnator;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BuildRainerTest {

	@Test
	/**
	 * This test if BuildRainer can connect
	 * and communicate with a ws server
	 */
	public void testWebSocket()
    {
    	TestServer testServer =TestServer.getInstance();
    	testServer.setReuseAddr(true);
        testServer.run();
        String receivedMsg = testServer.getBuildRainer().getRecentMessage();

        assertEquals(receivedMsg,"Test");
    }

    @Test
    /**
     * Test if ActiveMQSubmitter can successfully submit to queue
     */
    public void testActiveMQSubmitter()
    {
        ActiveMQBuildSubmitter submitter = new ActiveMQBuildSubmitter("tcp://localhost:61616","testQueue");
        submitter.submit("Test");
        assertEquals(submitter.receiveFromQueue(),"Test");
    }
}

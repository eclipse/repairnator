package fr.inria.repairnator;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import org.junit.Test;
import org.junit.Ignore;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BuildRainerTest {

	@Test
	/**
	 * This test if BuildRainer can connect
	 * and communicate with a ws server
     * Remove ignore to test, this can be a bit flaky.
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
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setActiveMQUrl("tcp://localhost:61616");
        config.setActiveMQSubmitQueueName("testQueue");
        config.setJmxHostName("localhost");
        config.setQueueLimit(1);

        ActiveMQBuildSubmitter submitter = new ActiveMQBuildSubmitter();
        submitter.initBroker();
        submitter.submit("Test");
        submitter.submit("WrongTest");
        assertEquals(submitter.receiveFromQueue(),"Test");
    }
}

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
        TestServer.getInstance().run();
        assertEquals(TestServer.getInstance().getRecentMessage(),"Hi");
    }
}

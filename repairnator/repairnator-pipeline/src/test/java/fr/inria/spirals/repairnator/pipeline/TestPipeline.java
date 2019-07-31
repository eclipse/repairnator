package fr.inria.spirals.repairnator.pipeline;

import com.martiansoftware.jsap.FlaggedOption;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestPipeline {

    @Test
    public void testPipelineArgs() throws Exception {

        // the default repair tool
        assertEquals(1, ((FlaggedOption)Launcher.defineArgs().getByLongFlag("repairTools")).getDefault().length);
        assertEquals("NPEFix", ((FlaggedOption)Launcher.defineArgs().getByLongFlag("repairTools")).getDefault()[0]);

        // non default value is accepted
        assertEquals("NopolAllTests", ((FlaggedOption)Launcher.defineArgs().getByLongFlag("repairTools")).getStringParser().parse("NopolAllTests"));

        // incorrect values are rejected
        try {
            ((FlaggedOption)Launcher.defineArgs().getByLongFlag("repairTools")).getStringParser().parse("garbage");
            fail();
        } catch (Exception expected) {}

    }

    @Test
    public void testPipeline() throws Exception {
        // requires env variable M2_HOME and GITHUB_OAUTH
        // (set in Travis config)
        // eg export M2_HOME=/usr/share/maven
        // from surli/failingBuild
        Launcher l = new Launcher(new String[]{"--build", "564711868"});
        l.mainProcess();
        assertEquals("PATCHED", l.getInspector().getFinding());
    }


	@Test
	public void testPRLuc12() throws Exception {
    	// reproducing the past PR of Luc
		Launcher l = new Launcher(new String[]{"--build", "395891390", "--repairTools", "NPEFix" });
		l.mainProcess();
		assertEquals("PATCHED", l.getInspector().getFinding());
	}

}

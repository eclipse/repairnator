package fr.inria.spirals.repairnator.pipeline;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestPipeline {

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
}

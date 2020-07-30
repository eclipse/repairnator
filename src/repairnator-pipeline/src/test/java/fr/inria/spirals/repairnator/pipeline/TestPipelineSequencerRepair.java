package fr.inria.spirals.repairnator.pipeline;

import com.martiansoftware.jsap.FlaggedOption;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestPipelineSequencerRepair {

    @Test
    public void TestPipelineBuildPassBranch() throws Exception{
        // 703887431 -> javierron/faining project -> test failure
        Launcher launcher = new Launcher(new String[]{"--build", "703887431", "--sequencerRepair" });
        // config is forced to use SequencerRepair as the only repair tool.
        assertEquals(1, launcher.getConfig().getRepairTools().size());
        assertTrue(launcher.getConfig().getRepairTools().contains("SequencerRepair"));

        launcher.mainProcess();

        AbstractStep step =  launcher.getInspector().getSteps().get(10); //test fix sequencer repair
        assertNull(step.getStepStatus());
    }

    @Test
    public void TestPipelineBuildFailBranch() throws Exception{
        // 703887431 -> javierron/faining project -> syntax error
        Launcher launcher = new Launcher(new String[]{"--build", "713361530", "--sequencerRepair" });
        // config is forced to use SequencerRepair as the only repair tool.
        assertEquals(1, launcher.getConfig().getRepairTools().size());
        assertTrue(launcher.getConfig().getRepairTools().contains("SequencerRepair"));

        launcher.mainProcess();

        AbstractStep step =  launcher.getInspector().getSteps().get(4); //syntax fix sequencer repair
        assertNull(step.getStepStatus());

    }

}

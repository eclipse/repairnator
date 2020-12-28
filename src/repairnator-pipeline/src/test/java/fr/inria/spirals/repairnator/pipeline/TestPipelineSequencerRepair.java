package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

import static org.junit.Assert.*;

public class TestPipelineSequencerRepair {

    @Rule
    public TemporaryFolder workspaceFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder outputFolder = new TemporaryFolder();

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void TestPipelineBuildPassBranch() throws Exception{
        // 703887431 -> javierron/faining project -> test failure
        Launcher launcher = new Launcher(new String[]{
                "--build",
                "703887431",
                "--sequencerRepair",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });
        // config is forced to use SequencerRepair as the only repair tool.
        assertEquals(1, launcher.getConfig().getRepairTools().size());
        assertTrue(launcher.getConfig().getRepairTools().contains("SequencerRepair"));

        launcher.mainProcess();

        AbstractStep step =  launcher.getInspector().getSteps().get(10); //test fix sequencer repair
        assertNull(step.getStepStatus());
    }

    @Test
    public void TestPipelineBuildFailBranch() throws Exception{
        // 713361530 -> javierron/faining project -> syntax error
        Launcher launcher = new Launcher(new String[]{
                "--build",
                "713361530",
                "--sequencerRepair",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });
        // config is forced to use SequencerRepair as the only repair tool.
        assertEquals(1, launcher.getConfig().getRepairTools().size());
        assertTrue(launcher.getConfig().getRepairTools().contains("SequencerRepair"));

        launcher.mainProcess();

        AbstractStep step =  launcher.getInspector().getSteps().get(4); //syntax fix sequencer repair
        assertNull(step.getStepStatus());

    }

}

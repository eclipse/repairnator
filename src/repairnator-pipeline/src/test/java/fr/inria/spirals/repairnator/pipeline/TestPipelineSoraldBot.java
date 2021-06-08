package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestPipelineSoraldBot {
    @Rule
    public TemporaryFolder workspaceFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder outputFolder = new TemporaryFolder();

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void TestPipelineSoraldRepairTool() throws Exception {
        Launcher launcher = new Launcher(new String[]{
                "--gitrepo",
                "--gitrepourl", "https://github.com/khaes-kth/Sorald-CI-Sample",
                "--gitcommithash", "e2e0e568412cd05efb4475715f457473b3777437",
                "--sonarRules", "1217",
                "--repairTools", "SoraldBot",
                "--launcherMode", "GIT_REPOSITORY",
                "--createPR", "true",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });

        // config is forced to use SequencerRepair as the only repair tool.
        assertEquals(1, launcher.getConfig().getRepairTools().size());
        assertTrue(launcher.getConfig().getRepairTools().contains("SoraldBot"));

        launcher.mainProcess();

        List<AbstractStep> steps =  launcher.getInspector().getSteps()
                .stream()
                .filter(step -> step.getName().equals("SoraldBot"))
                .collect(Collectors.toList()); //test fix sequencer repair

        assertEquals(1, steps.size());
    }
}

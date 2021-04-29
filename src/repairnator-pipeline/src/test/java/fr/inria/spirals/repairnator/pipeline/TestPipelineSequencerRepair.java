package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

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
    public void TestPipelineSequencerRepairTool() throws Exception {
        // 220926535 -> repairnator/failingProject -> test failure
        Launcher launcher = new Launcher(new String[]{
                "--jtravisendpoint", "https://api.travis-ci.com",
                "--build", "220926535",
                "--sequencerRepair",
                "--gitrepo",
                "--gitrepourl", "https://github.com/javierron/failingProject",
                "--gitrepoidcommit", "ec915681fbd6a8b2c30580b2618e62636204abe4",
                "--launcherMode", "SEQUENCER_REPAIR",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });

        // config is forced to use SequencerRepair as the only repair tool.
        assertEquals(1, launcher.getConfig().getRepairTools().size());
        assertTrue(launcher.getConfig().getRepairTools().contains("SequencerRepair"));

        launcher.mainProcess();

        List<AbstractStep> steps =  launcher.getInspector().getSteps()
                .stream()
                .filter(step -> step.getName().equals("SequencerRepair"))
                .collect(Collectors.toList()); //test fix sequencer repair

        assertEquals(2, steps.size());
    }

    @Test
    public void TestPipelineBuildPassBranch() throws Exception{
        // e182ccb9ef41b5adab602ed12bfc71b744ff0241 -> javierron/faining project -> test failure
        Launcher launcher = new Launcher(new String[]{
                "--sequencerRepair",
                "--gitrepo",
                "--gitrepourl", "https://github.com/javierron/failingProject",
                "--gitrepoidcommit", "e182ccb9ef41b5adab602ed12bfc71b744ff0241",
                "--launcherMode", "SEQUENCER_REPAIR",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });

        launcher.mainProcess();

        AbstractStep step =  launcher.getInspector().getSteps().get(4); //test fix sequencer repair
        assertNotNull(step.getStepStatus());
    }

    @Test
    public void TestPipelineBuildFailBranch() throws Exception{
        // 713361530 -> repairnator/failingProject -> syntax error
        Launcher launcher = new Launcher(new String[]{
                "--jtravisendpoint", "https://api.travis-ci.com",
                "--build", "220941672",
                "--sequencerRepair",
                "--gitrepo",
                "--gitrepourl", "https://github.com/javierron/failingProject",
                "--gitrepoidcommit", "ec915681fbd6a8b2c30580b2618e62636204abe4",
                "--launcherMode", "SEQUENCER_REPAIR",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });

        launcher.mainProcess();

        AbstractStep step =  launcher.getInspector().getSteps().get(9); //syntax fix sequencer repair
        assertNotNull(step.getStepStatus());

    }

}

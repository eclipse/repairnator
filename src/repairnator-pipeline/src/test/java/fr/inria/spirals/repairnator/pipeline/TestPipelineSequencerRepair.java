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

/**
 * Dear maintainer, the CI on Travis might fail or process this test too late because the information of the test
 * results stops existing after an amount of time. If this happens go to repainator/failinProject repository and
 * rerun the jobs of the commits used below. For this you need the autorization of repairnator Github organization,
 * so just ask @monperrus for this and you will have the power to do this.
 * Last time jobs where run: 2023-01-13 by @Sofi1410
 */
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
    @Ignore
    //FIXME: We can't rely on repairnator/failing project to get builds
    public void TestPipelineSequencerRepairTool() throws Exception {
        // ec915681fbd6a8b2c30580b2618e62636204abe4 -> repairnator/failingProject -> syntax
        Launcher launcher = new Launcher(new String[]{
                "--sequencerRepair",
                "--gitrepourl", "https://github.com/repairnator/failingProject",
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
    @Ignore
    //FIXME: We can't rely on repairnator/failing project to get builds
    public void TestPipelineBuildPassBranch() throws Exception{
        // e182ccb9ef41b5adab602ed12bfc71b744ff0241 -> repairnator/failingProject -> nofixes
        Launcher launcher = new Launcher(new String[]{
                "--sequencerRepair",
                "--gitrepourl", "https://github.com/repairnator/failingProject",
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
    @Ignore
    //FIXME: We can't rely on repairnator/failing project to get builds
    public void TestPipelineBuildFailBranch() throws Exception{
        // ec915681fbd6a8b2c30580b2618e62636204abe4 -> repairnator/failingProject -> syntax error
        Launcher launcher = new Launcher(new String[]{
                "--gitrepourl", "https://github.com/repairnator/failingProject",
                "--gitrepoidcommit", "ec915681fbd6a8b2c30580b2618e62636204abe4",
                "--sequencerRepair",
                "--launcherMode", "SEQUENCER_REPAIR",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });

        launcher.mainProcess();

        AbstractStep step =  launcher.getInspector().getSteps().get(9); //syntax fix sequencer repair
        assertNotNull(step.getStepStatus());

    }

}

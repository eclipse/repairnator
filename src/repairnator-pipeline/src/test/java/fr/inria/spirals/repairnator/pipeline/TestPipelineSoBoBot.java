package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TestPipelineSoBoBot {
    @Rule
    public TemporaryFolder workspaceFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder outputFolder = new TemporaryFolder();

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void TestPipelineSoboFeedbackTool() throws Exception {
        Launcher launcher = new Launcher(new String[]{
                "--gitrepourl", "https://gits-15.sys.kth.se/inda-19/linusost-task-11",
                "--gitcommithash", "10b75090bf93ecae250ace247cf1815c7c5b084b",
                "--sonarRules", "S109",
                "--feedbackTools", "SoboBot",
                "--launcherMode", "FEEDBACK", //not sure if feedback or git_repository
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });

        // config is forced to use SoraldBot as the only repair tool.
        assertEquals(0, launcher.getConfig().getRepairTools().size());
        assertEquals(1, launcher.getConfig().getFeedbackTools().size());
        assertTrue(launcher.getConfig().getFeedbackTools().contains("SoboBot"));
        assertFalse(launcher.getConfig().getRepairTools().contains("SoboBot"));


        Patches patchNotifier = new Patches();
        launcher.setPatchNotifier(patchNotifier);

        launcher.mainProcess();

        List<AbstractStep> steps = launcher.getInspector().getSteps()
                .stream()
                .filter(step -> step.getName().equals("SoboBot"))
                .collect(Collectors.toList()); //test fix sorald-bot repair

        assertEquals(1, steps.size());
    }
    class Patches implements PatchNotifier {
        List<RepairPatch> allpatches = new ArrayList<>();

        @Override
        public void notify(ProjectInspector inspector, String toolname, List<RepairPatch> patches) {
            allpatches.addAll(patches);
        }
    }

}

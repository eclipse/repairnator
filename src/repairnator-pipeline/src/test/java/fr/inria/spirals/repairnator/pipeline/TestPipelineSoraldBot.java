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
                "--gitrepourl", "https://github.com/khaes-kth/Sorald-CI-Sample",
                "--gitcommithash", "e2e0e568412cd05efb4475715f457473b3777437",
                "--sonarRules", "1217",
                "--repairTools", "SoraldBot",
                "--launcherMode", "GIT_REPOSITORY",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });

        // config is forced to use SoraldBot as the only repair tool.
        assertEquals(1, launcher.getConfig().getRepairTools().size());
        assertTrue(launcher.getConfig().getRepairTools().contains("SoraldBot"));

        Patches patchNotifier = new Patches();
        launcher.setPatchNotifier(patchNotifier);

        launcher.mainProcess();

        List<AbstractStep> steps =  launcher.getInspector().getSteps()
                .stream()
                .filter(step -> step.getName().equals("SoraldBot"))
                .collect(Collectors.toList()); //test fix sorald-bot repair

        assertEquals(1, steps.size());
        assertEquals(1, patchNotifier.allpatches.size());
    }

    class Patches implements PatchNotifier {
        List<RepairPatch> allpatches = new ArrayList<>();

        @Override
        public void notify(ProjectInspector inspector, String toolname, List<RepairPatch> patches) {
            allpatches.addAll(patches);
        }
    }
}

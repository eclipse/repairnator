package fr.inria.spirals.repairnator.pipeline;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.faultLocalization.FlacocoLocalization;
import fr.inria.spirals.repairnator.process.step.push.PushFaultLocalizationSuggestions;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class TestPipelineFaultLocalization {

    @Rule
    public TemporaryFolder workspaceFolder = new TemporaryFolder();

    @Rule
    public TemporaryFolder outputFolder = new TemporaryFolder();

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
    }

    /**
     * No results are pushed because the default threshold is higher than any of the lines executed
     * inside the actual diff. This is meant to be, since we cannot test this feature in CI easily.
     * @throws Exception
     */
    @Test
    public void testPipelineFaultLocalization() throws Exception {
        Launcher launcher = new Launcher(new String[]{
                "--gitrepourl", "https://github.com/repairnator/failingProject",
                "--gitrepopullrequest", "7",
                "--flacocoThreshold", "1.0", // This threshold results in 0 lines, and so the result is not pushed
                "--faultLocalization",
                "--launcherMode", "FAULT_LOCALIZATION",
                "--workspace", workspaceFolder.getRoot().getAbsolutePath(),
                "--output", outputFolder.getRoot().getAbsolutePath()
        });

        launcher.mainProcess();

        // we check that the pipeline has been built for fault localization
        List<AbstractStep> steps = launcher.getInspector().getSteps();

        assertThat(steps.size(), is(9));
        assertThat(steps.get(7), instanceOf(FlacocoLocalization.class));
        assertThat(steps.get(8), instanceOf(PushFaultLocalizationSuggestions.class));
    }

}

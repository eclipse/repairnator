package fr.inria.spirals.repairnator.process.step.faultLocalization;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.utils.Utils;
import fr.spoonlabs.flacoco.api.result.Location;
import fr.spoonlabs.flacoco.api.result.Suspiciousness;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestFlacocoLocalization {

    @Rule
    public TemporaryFolder workspaceFolder = new TemporaryFolder();

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.ERROR);
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setJTravisEndpoint("https://api.travis-ci.com");
        config.setFlacocoThreshold(0.5);
    }

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void testFlacocoLocalization() throws IOException {
        long buildId = 236072272; // repairnator/failingProject build

        Build build = this.checkBuildAndReturn(buildId, true);

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        ProjectInspector inspector = new ProjectInspector(toBeInspected, workspaceFolder.getRoot().getAbsolutePath(), null, null);

        CloneRepository cloneStep = new CloneRepository(inspector);
        FlacocoLocalization flacocoLocalization = new FlacocoLocalization(inspector, true);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true))
                .addNextStep(new TestProject(inspector))
                .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
                .addNextStep(new ComputeClasspath(inspector, true))
                .addNextStep(new ComputeSourceDir(inspector, true, false))
                .addNextStep(new ComputeTestDir(inspector, true))
                .addNextStep(flacocoLocalization);
        cloneStep.execute();

        assertThat(flacocoLocalization.isShouldStop(), is(false));

        List<StepStatus> stepStatusList = inspector.getJobStatus().getStepStatuses();
        assertThat(stepStatusList.size(), is(8));
        StepStatus assertFixerStatus = stepStatusList.get(7);
        assertThat(assertFixerStatus.getStep(), is(flacocoLocalization));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat("Failing step :" + stepStatus, stepStatus.isSuccess(), is(true));
        }

        // assert that fault localization results are stored
        Map<Location, Suspiciousness> results = inspector.getJobStatus().getFlacocoResult().getDefaultSuspiciousnessMap();
        assertThat(results, notNullValue());
        assertThat(results.size(), is(4));

        assertThat(results.get(new Location("nopol_examples.nopol_example_3.NopolExample", 3)), notNullValue());
        assertThat(results.get(new Location("nopol_examples.nopol_example_3.NopolExample", 9)), notNullValue());
        assertThat(results.get(new Location("nopol_examples.nopol_example_3.NopolExample", 10)), notNullValue());
        assertThat(results.get(new Location("nopol_examples.nopol_example_3.NopolExample", 12)), notNullValue());
    }

    private Build checkBuildAndReturn(long buildId, boolean isPR) {
        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());

        Build build = optionalBuild.get();
        assertThat(build, IsNull.notNullValue());
        assertThat(buildId, Is.is(build.getId()));
        assertThat(build.isPullRequest(), Is.is(isPR));

        return build;
    }

}

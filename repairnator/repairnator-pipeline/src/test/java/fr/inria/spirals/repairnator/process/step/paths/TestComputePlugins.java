package fr.inria.spirals.repairnator.process.step.paths;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.utils.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.files.FileHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutType;
import fr.inria.spirals.repairnator.process.utils4tests.ProjectInspectorMocker;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestComputePlugins {

    private File tmpDir;

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.ERROR);
    }

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
        FileHelper.deleteFile(tmpDir);
    }

    @Test
    public void testComputePluginsWithSingleModuleProject() throws IOException {
        long buggyBuildCandidateId = 380717778; // https://travis-ci.org/eclipse/repairnator/builds/380717778

        Build buggyBuildCandidate = this.checkBuildAndReturn(buggyBuildCandidateId, false);

        BuildToBeInspected buildToBeInspected = new BuildToBeInspected(buggyBuildCandidate, null, ScannedBuildStatus.ONLY_FAIL, "test");

        tmpDir = Files.createTempDirectory("test_compute_plugins_with_single_module_project").toFile();

        File repoDir = new File(tmpDir, "repo");
        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, buildToBeInspected, CheckoutType.CHECKOUT_BUGGY_BUILD);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputePlugins computePlugins = new ComputePlugins(inspector, true);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true)).addNextStep(computePlugins);
        cloneStep.execute();

        assertThat(computePlugins.isShouldStop(), is(false));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus classpathStatus = stepStatusList.get(2);
        assertThat(classpathStatus.getStep(), is(computePlugins));
        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertTrue(jobStatus.getProperties().getProjectMetrics().getNumberPlugins() >= 7);
    }

    @Test
    public void testComputePluginsWithNoPlugin() throws IOException {
        long buggyBuildCandidateId = 352395977; // https://travis-ci.org/surli/test-repairnator/builds/352395977

        Build buggyBuildCandidate = this.checkBuildAndReturn(buggyBuildCandidateId, false);

        BuildToBeInspected buildToBeInspected = new BuildToBeInspected(buggyBuildCandidate, null, ScannedBuildStatus.ONLY_FAIL, "test");

        tmpDir = Files.createTempDirectory("test_compute_plugins_with_single_module_project").toFile();

        File repoDir = new File(tmpDir, "repo");
        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, buildToBeInspected, CheckoutType.CHECKOUT_BUGGY_BUILD);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputePlugins computePlugins = new ComputePlugins(inspector, true);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true)).addNextStep(computePlugins);
        cloneStep.execute();

        assertThat(computePlugins.isShouldStop(), is(false));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus classpathStatus = stepStatusList.get(2);
        assertThat(classpathStatus.getStep(), is(computePlugins));
        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertThat(jobStatus.getProperties().getProjectMetrics().getNumberPlugins(), is(0));
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

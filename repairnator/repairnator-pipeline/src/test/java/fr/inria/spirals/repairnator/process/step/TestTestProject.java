package fr.inria.spirals.repairnator.process.step;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
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
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by urli on 07/03/2017.
 */
public class TestTestProject {

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.ERROR);
    }

    @After
    public void tearDown() {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void testTestProjectWhenFailing() throws IOException {
        long buildId = 207890790; // surli/failingProject build

        Build build = this.checkBuildAndReturn(buildId, false);

        Path tmpDirPath = Files.createTempDirectory("test_testproject");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected, CheckoutType.CHECKOUT_BUGGY_BUILD);

        CloneRepository cloneStep = new CloneRepository(inspector);
        TestProject testProject = new TestProject(inspector);


        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector, true)).setNextStep(testProject);
        cloneStep.execute();

        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus testStatus = stepStatusList.get(2);
        assertThat(testStatus.getStep(), is(testProject));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }
    }

    @Test
    public void testTestProjectWhenErroring() throws IOException {
        long buildId = 208240908; // surli/failingProject build

        Build build = this.checkBuildAndReturn(buildId, false);

        Path tmpDirPath = Files.createTempDirectory("test_clone");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected, CheckoutType.CHECKOUT_BUGGY_BUILD);

        CloneRepository cloneStep = new CloneRepository(inspector);
        TestProject testProject = new TestProject(inspector);


        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector, true)).setNextStep(testProject);
        cloneStep.execute();

        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus testStatus = stepStatusList.get(2);
        assertThat(testStatus.getStep(), is(testProject));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }
    }

    @Test
    public void testTestProjectWhenNotFailing() throws IOException {
        long buildId = 201176013; // surli/failingProject build

        Build build = this.checkBuildAndReturn(buildId, false);

        Path tmpDirPath = Files.createTempDirectory("test_clone");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected, CheckoutType.CHECKOUT_BUGGY_BUILD);

        CloneRepository cloneStep = new CloneRepository(inspector);
        TestProject testProject = new TestProject(inspector);


        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector, true)).setNextStep(testProject);
        cloneStep.execute();

        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus testStatus = stepStatusList.get(2);
        assertThat(testStatus.getStep(), is(testProject));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }
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

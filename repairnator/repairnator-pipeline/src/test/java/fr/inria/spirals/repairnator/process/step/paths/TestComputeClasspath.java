package fr.inria.spirals.repairnator.process.step.paths;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.TestProject;
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
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by urli on 07/03/2017.
 */
public class TestComputeClasspath {

    private File tmpDir;

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.ERROR);
    }

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
        GitHelper.deleteFile(tmpDir);
    }

    @Test
    public void testComputeClasspathWithSingleModuleProject() throws IOException {
        long buildId = 201176013; // surli/failingProject build

        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_computecp").toFile();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected, CheckoutType.CHECKOUT_BUGGY_BUILD);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeClasspath computeClasspath = new ComputeClasspath(inspector, true);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector, true))
                .setNextStep(new TestProject(inspector))
                .setNextStep(computeClasspath);
        cloneStep.execute();

        assertThat(computeClasspath.isShouldStop(), is(false));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(4));
        StepStatus classpathStatus = stepStatusList.get(3);
        assertThat(classpathStatus.getStep(), is(computeClasspath));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        List<URL> expectedClasspath = new ArrayList<URL>();

        URL junit = new URL("file:"+tmpDir.getAbsolutePath()+"/.m2/junit/junit/4.11/junit-4.11.jar");
        URL hamcrest = new URL("file:"+tmpDir.getAbsolutePath()+"/.m2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar");
        URL classDir = new URL("file:"+repoDir.getAbsolutePath()+"/target/classes/");
        URL testDir = new URL("file:"+repoDir.getAbsolutePath()+"/target/test-classes/");

        expectedClasspath.add(junit);
        expectedClasspath.add(hamcrest);
        expectedClasspath.add(classDir);
        expectedClasspath.add(testDir);

        assertThat(jobStatus.getRepairClassPath(), is(expectedClasspath));
        assertThat(jobStatus.getMetrics().getNbLibraries(), is(2));
        assertThat(jobStatus.getMetrics4Bears().getProjectMetrics().getNumberLibrariesFailingModule(), is(2));
    }

    @Test
    public void testComputeClasspathWithMultiModuleProject() throws IOException {
        long buggyBuildCandidateId = 386269112; // https://travis-ci.org/fermadeiral/test-repairnator-bears/builds/386269112

        Build buggyBuildCandidate = this.checkBuildAndReturn(buggyBuildCandidateId, false);

        tmpDir = Files.createTempDirectory("test_computecp").toFile();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(buggyBuildCandidate, null, ScannedBuildStatus.ONLY_FAIL, "");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath() + File.separator + "test-repairnator-bears-core");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected, CheckoutType.CHECKOUT_BUGGY_BUILD);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeClasspath computeClasspath = new ComputeClasspath(inspector, true);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector, true))
                .setNextStep(new TestProject(inspector))
                .setNextStep(computeClasspath);
        cloneStep.execute();

        assertThat(computeClasspath.isShouldStop(), is(false));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(4));
        StepStatus classpathStatus = stepStatusList.get(3);
        assertThat(classpathStatus.getStep(), is(computeClasspath));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        List<URL> expectedClasspath = new ArrayList<URL>();

        expectedClasspath.add(new URL("file:"+tmpDir.getAbsolutePath()+"/.m2/junit/junit/4.12/junit-4.12.jar"));
        expectedClasspath.add(new URL("file:"+tmpDir.getAbsolutePath()+"/.m2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar"));
        expectedClasspath.add(new URL("file:"+jobStatus.getFailingModulePath()+"/target/classes/"));
        expectedClasspath.add(new URL("file:"+jobStatus.getFailingModulePath()+"/target/test-classes/"));

        assertThat(jobStatus.getRepairClassPath(), is(expectedClasspath));
        assertThat(jobStatus.getMetrics().getNbLibraries(), is(2));
        assertThat(jobStatus.getMetrics4Bears().getProjectMetrics().getNumberLibrariesFailingModule(), is(2));
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

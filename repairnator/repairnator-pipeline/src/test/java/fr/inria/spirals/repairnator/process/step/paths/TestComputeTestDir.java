package fr.inria.spirals.repairnator.process.step.paths;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.files.FileHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
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

/**
 * Created by urli on 07/03/2017.
 */
public class TestComputeTestDir {

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
    public void testComputeTestDirWithMultiModuleProject() throws IOException {
        long buggyBuildCandidateId = 386332218; // https://travis-ci.org/fermadeiral/test-repairnator-bears/builds/386332218

        Build buggyBuildCandidate = this.checkBuildAndReturn(buggyBuildCandidateId, false);

        BuildToBeInspected buildToBeInspected = new BuildToBeInspected(buggyBuildCandidate, null, ScannedBuildStatus.ONLY_FAIL, "test");

        tmpDir = Files.createTempDirectory("computetestdir").toFile();

        File repoDir = new File(tmpDir, "repo");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, buildToBeInspected, CheckoutType.CHECKOUT_BUGGY_BUILD);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeTestDir computeTestDir = new ComputeTestDir(inspector, true);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true)).addNextStep(computeTestDir);
        cloneStep.execute();

        assertThat(computeTestDir.isShouldStop(), is(false));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus computeTestDirStatus = stepStatusList.get(2);
        assertThat(computeTestDirStatus.getStep(), is(computeTestDir));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertThat(jobStatus.getTestDir(), is(new File[] {
                new File(repoDir.getAbsolutePath()+"/test-repairnator-bears-core/src/test/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/test-repairnator-bears-patchstats/src/test/java").getCanonicalFile()
        }));
        assertThat(jobStatus.getProperties().getProjectMetrics().getNumberTestFiles(), is(3));
    }

    @Test
    public void testComputeTestDirWithReflexiveReferences() throws IOException {
        long buildId = 345990212;

        Build build = this.checkBuildAndReturn(buildId, true);

        tmpDir = Files.createTempDirectory("computetestdir").toFile();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeTestDir computeTestDir = new ComputeTestDir(inspector, true);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true)).addNextStep(computeTestDir);
        cloneStep.execute();

        assertThat(computeTestDir.isShouldStop(), is(true));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus computeTestDirStatus = stepStatusList.get(2);
        assertThat(computeTestDirStatus.getStep(), is(computeTestDir));

        for (StepStatus stepStatus : stepStatusList) {
            if (stepStatus.getStep() != computeTestDir) {
                assertThat(stepStatus.isSuccess(), is(true));
            } else {
                assertThat(stepStatus.isSuccess(), is(false));
            }

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

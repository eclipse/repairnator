package fr.inria.spirals.repairnator.process.step.paths;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
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
public class TestComputeTestDir {

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.ERROR);
    }

    @After
    public void tearDown() {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void testComputeTestDirWithReflexiveReferences() throws IOException {
        long buildId = 345990212;

        Build build = this.checkBuildAndReturn(buildId, true);

        Path tmpDirPath = Files.createTempDirectory("computetestdir");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeTestDir computeTestDir = new ComputeTestDir(inspector, true);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector, true)).setNextStep(computeTestDir);
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

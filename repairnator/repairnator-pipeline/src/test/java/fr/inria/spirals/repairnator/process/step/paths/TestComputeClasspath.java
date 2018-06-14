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
import java.nio.file.Path;
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

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.ERROR);
    }

    @After
    public void tearDown() {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void testComputeClasspath() throws IOException {
        long buildId = 201176013; // surli/failingProject build

        Build build = this.checkBuildAndReturn(buildId, false);

        Path tmpDirPath = Files.createTempDirectory("test_computecp");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

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

        URL classDir = new URL("file:"+repoDir.getAbsolutePath()+"/target/classes/");
        URL testDir = new URL("file:"+repoDir.getAbsolutePath()+"/target/test-classes/");
        URL junit = new URL("file:"+tmpDir.getAbsolutePath()+"/.m2/junit/junit/4.11/junit-4.11.jar");
        URL hamcrest = new URL("file:"+tmpDir.getAbsolutePath()+"/.m2/org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar");

        expectedClasspath.add(classDir);
        expectedClasspath.add(testDir);
        expectedClasspath.add(junit);
        expectedClasspath.add(hamcrest);

        assertThat(jobStatus.getRepairClassPath(), is(expectedClasspath));
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

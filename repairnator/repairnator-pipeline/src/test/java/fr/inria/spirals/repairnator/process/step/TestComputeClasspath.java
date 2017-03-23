package fr.inria.spirals.repairnator.process.step;

import ch.qos.logback.classic.Level;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.ScannedBuildStatus;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuild;
import fr.inria.spirals.repairnator.process.step.gatherinfocontract.GatherTestInformation;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by urli on 07/03/2017.
 */
public class TestComputeClasspath {

    static {
        Utils.setLoggersLevel(Level.ERROR);
    }

    @Test
    public void testComputeClasspath() throws IOException {
        int buildId = 201176013; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computecp");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeClasspath computeClasspath = new ComputeClasspath(inspector);

        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(new TestProject(inspector)).setNextStep(computeClasspath);
        cloneStep.execute();

        assertThat(computeClasspath.shouldStop, is(false));
        assertThat(computeClasspath.getState(), is(ProjectState.CLASSPATHCOMPUTED));
        assertThat(jobStatus.getState(), is(ProjectState.CLASSPATHCOMPUTED));

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
}

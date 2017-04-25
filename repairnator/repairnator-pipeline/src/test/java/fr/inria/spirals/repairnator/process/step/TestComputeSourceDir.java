package fr.inria.spirals.repairnator.process.step;

import ch.qos.logback.classic.Level;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.ScannedBuildStatus;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuild;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by urli on 07/03/2017.
 */
public class TestComputeSourceDir {

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.ERROR);
    }

    @After
    public void tearDown() {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void testComputeSourceDir() throws IOException {
        int buildId = 207924136; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computesourcedir");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getPatchedBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector);

        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(new TestProject(inspector)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.shouldStop, is(false));
        assertThat(computeSourceDir.getState(), is(ProjectState.SOURCEDIRCOMPUTED));
        assertThat(jobStatus.getState(), is(ProjectState.SOURCEDIRCOMPUTED));

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {new File(repoDir.getAbsolutePath()+"/src/main/java")}));
    }

    @Test
    public void testComputeSourceDirWithMultiModuleProject() throws IOException {
        int buildId = 225251586; // Spirals-Team/librepair build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computesourcedir2");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getPatchedBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath()+"/test-projects");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector);

        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.shouldStop, is(false));
        assertThat(computeSourceDir.getState(), is(ProjectState.SOURCEDIRCOMPUTED));
        assertThat(jobStatus.getState(), is(ProjectState.SOURCEDIRCOMPUTED));

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {new File(repoDir.getAbsolutePath()+"/test-projects/src/main/java")}));
    }

    @Test
    public void testComputeSourceDirWithMultiModuleProject2() throws IOException {
        int buildId = 225251586; // Spirals-Team/librepair build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computesourcedir2");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getPatchedBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath()+"/a-module");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector);

        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.shouldStop, is(false));
        assertThat(computeSourceDir.getState(), is(ProjectState.SOURCEDIRCOMPUTED));
        assertThat(jobStatus.getState(), is(ProjectState.SOURCEDIRCOMPUTED));

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {new File(repoDir.getAbsolutePath()+"/a-module/src/custom/folder")}));
    }

    @Test
    public void testComputeSourceDirWithMultiModuleProject3() throws IOException {
        int buildId = 225251586; // Spirals-Team/librepair build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computesourcedir2");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getPatchedBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector);

        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.shouldStop, is(false));
        assertThat(computeSourceDir.getState(), is(ProjectState.SOURCEDIRCOMPUTED));
        assertThat(jobStatus.getState(), is(ProjectState.SOURCEDIRCOMPUTED));

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {new File(repoDir.getAbsolutePath()+"/a-module/src/custom/folder"), new File(repoDir.getAbsolutePath()+"/test-projects/src/main/java")}));
    }
}

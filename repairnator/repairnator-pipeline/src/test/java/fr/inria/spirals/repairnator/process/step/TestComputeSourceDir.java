package fr.inria.spirals.repairnator.process.step;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutPatchedBuild;
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
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, false);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector)).setNextStep(new TestProject(inspector)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.shouldStop, is(false));
        assertThat(computeSourceDir.getPipelineState(), is(PipelineState.SOURCEDIRCOMPUTED));
        assertThat(jobStatus.getPipelineState(), is(PipelineState.SOURCEDIRCOMPUTED));

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
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath()+"/test-projects");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, false);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.shouldStop, is(false));
        assertThat(computeSourceDir.getPipelineState(), is(PipelineState.SOURCEDIRCOMPUTED));
        assertThat(jobStatus.getPipelineState(), is(PipelineState.SOURCEDIRCOMPUTED));

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
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath()+"/a-module");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, false);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.shouldStop, is(false));
        assertThat(computeSourceDir.getPipelineState(), is(PipelineState.SOURCEDIRCOMPUTED));
        assertThat(jobStatus.getPipelineState(), is(PipelineState.SOURCEDIRCOMPUTED));

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
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, false);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.shouldStop, is(false));
        assertThat(computeSourceDir.getPipelineState(), is(PipelineState.SOURCEDIRCOMPUTED));
        assertThat(jobStatus.getPipelineState(), is(PipelineState.SOURCEDIRCOMPUTED));

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {new File(repoDir.getAbsolutePath()+"/a-module/src/custom/folder"), new File(repoDir.getAbsolutePath()+"/test-projects/src/main/java")}));
    }

    @Test
    public void testComputeSourceDirWithMultiModuleProject4() throws IOException {
        int buildId = 216674182; // pac4j/pac4j
        int patchedBuildId = 218753299;

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Build patchedBuild = BuildHelper.getBuildFromId(patchedBuildId, null);
        assertThat(patchedBuild, notNullValue());
        assertThat(patchedBuildId, is(patchedBuild.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computesourcedir3");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, patchedBuild, ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getPatchedBuild()).thenReturn(patchedBuild);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, false);

        cloneStep.setNextStep(new CheckoutPatchedBuild(inspector)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.shouldStop, is(false));
        assertThat(computeSourceDir.getPipelineState(), is(PipelineState.SOURCEDIRCOMPUTED));
        assertThat(jobStatus.getPipelineState(), is(PipelineState.SOURCEDIRCOMPUTED));

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {
                new File(repoDir.getAbsolutePath()+"/pac4j-core/src/main/java"),
                new File(repoDir.getAbsolutePath()+"/pac4j-config/src/main/java"),
                new File(repoDir.getAbsolutePath()+"/pac4j-oauth/src/main/java"),
                new File(repoDir.getAbsolutePath()+"/pac4j-cas/src/main/java"),
                new File(repoDir.getAbsolutePath()+"/pac4j-openid/src/main/java"),
                new File(repoDir.getAbsolutePath()+"/pac4j-http/src/main/java"),
                new File(repoDir.getAbsolutePath()+"/pac4j-saml/src/main/java"),
                new File(repoDir.getAbsolutePath()+"/pac4j-gae/src/main/java"),
                new File(repoDir.getAbsolutePath()+"/pac4j-oidc/src/main/java"),
                new File(repoDir.getAbsolutePath()+"/pac4j-jwt/src/main/java"),
                new File(repoDir.getAbsolutePath()+"/pac4j-ldap/src/main/java"),
                new File(repoDir.getAbsolutePath()+"/pac4j-sql/src/main/java"),
                new File(repoDir.getAbsolutePath()+"/pac4j-mongo/src/main/java"),
                new File(repoDir.getAbsolutePath()+"/pac4j-stormpath/src/main/java")
        }));
    }

    @Test
    public void testComputeSourceDirWithMultiModuleProject5() throws IOException {
        int buildId = 218168470; // Spirals-Team/librepair build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computesourcedir2");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, false);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.shouldStop, is(false));
        assertThat(computeSourceDir.getPipelineState(), is(PipelineState.SOURCEDIRCOMPUTED));
        assertThat(jobStatus.getPipelineState(), is(PipelineState.SOURCEDIRCOMPUTED));

        //assertThat(jobStatus.getRepairSourceDir(), is(new File[] {new File(repoDir.getAbsolutePath()+"/a-module/src/custom/folder"), new File(repoDir.getAbsolutePath()+"/test-projects/src/main/java")}));
    }

    @Test
    public void testComputeSourceDirWithReflexiveReferences() throws IOException {
        int buildId = 345990212;
        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computesourcedirOverflow");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, false);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.shouldStop, is(false));
        assertThat(computeSourceDir.getPipelineState(), is(PipelineState.SOURCEDIRNOTCOMPUTED));
        assertThat(jobStatus.getPipelineState(), is(PipelineState.SOURCEDIRNOTCOMPUTED));

    }
}

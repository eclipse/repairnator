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
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutPatchedBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutType;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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
        long buildId = 207924136; // surli/failingProject build

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computesourcedir");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getRepoSlug()).thenReturn(toBeInspected.getBuggyBuild().getRepository().getSlug());
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());
        when(inspector.getCheckoutType()).thenReturn(CheckoutType.CHECKOUT_BUGGY_BUILD);

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, true, false);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector, true))
                .setNextStep(new TestProject(inspector))
                .setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.isShouldStop(), is(false));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(4));
        StepStatus computeSourceDirStatus = stepStatusList.get(3);
        assertThat(computeSourceDirStatus.getStep(), is(computeSourceDir));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {new File(repoDir.getAbsolutePath()+"/src/main/java")}));
    }

    @Test
    public void testComputeSourceDirWithMultiModuleProject() throws IOException {
        long buildId = 225251586; // Spirals-Team/librepair build

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computesourcedir2");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getRepoSlug()).thenReturn(toBeInspected.getBuggyBuild().getRepository().getSlug());
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
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, true, false);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector, true))
                .setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.isShouldStop(), is(false));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus computeSourceDirStatus = stepStatusList.get(2);
        assertThat(computeSourceDirStatus.getStep(), is(computeSourceDir));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {new File(repoDir.getAbsolutePath()+"/test-projects/src/main/java")}));
    }

    @Test
    public void testComputeSourceDirWithMultiModuleProject2() throws IOException {
        long buildId = 225251586; // Spirals-Team/librepair build

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computesourcedir2");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getRepoSlug()).thenReturn(toBeInspected.getBuggyBuild().getRepository().getSlug());
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
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, true, false);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector, true)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.isShouldStop(), is(false));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus computeSourceDirStatus = stepStatusList.get(2);
        assertThat(computeSourceDirStatus.getStep(), is(computeSourceDir));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {new File(repoDir.getAbsolutePath()+"/a-module/src/custom/folder")}));
    }

    @Test
    public void testComputeSourceDirWithMultiModuleProject3() throws IOException {
        long buildId = 225251586; // Spirals-Team/librepair build

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computesourcedir2");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getRepoSlug()).thenReturn(toBeInspected.getBuggyBuild().getRepository().getSlug());
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
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, true, false);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector, true)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.isShouldStop(), is(false));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus computeSourceDirStatus = stepStatusList.get(2);
        assertThat(computeSourceDirStatus.getStep(), is(computeSourceDir));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {new File(repoDir.getAbsolutePath()+"/a-module/src/custom/folder"), new File(repoDir.getAbsolutePath()+"/test-projects/src/main/java")}));
    }

    @Test
    public void testComputeSourceDirWithMultiModuleProject4() throws IOException {
        long buildId = 216674182; // pac4j/pac4j
        long patchedBuildId = 218753299;

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Optional<Build> optionalPatchedBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(patchedBuildId);
        assertTrue(optionalPatchedBuild.isPresent());
        Build patchedBuild = optionalPatchedBuild.get();
        assertThat(patchedBuild, notNullValue());
        assertThat(patchedBuildId, is(patchedBuild.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computesourcedir3");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, patchedBuild, ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getRepoSlug()).thenReturn(toBeInspected.getBuggyBuild().getRepository().getSlug());
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
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, true, false);

        cloneStep.setNextStep(new CheckoutPatchedBuild(inspector, true)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.isShouldStop(), is(false));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus computeSourceDirStatus = stepStatusList.get(2);
        assertThat(computeSourceDirStatus.getStep(), is(computeSourceDir));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

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
        long buildId = 218168470; // Spirals-Team/librepair build

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computesourcedir2");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getRepoSlug()).thenReturn(toBeInspected.getBuggyBuild().getRepository().getSlug());
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
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, true, false);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector, true)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.isShouldStop(), is(false));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus computeSourceDirStatus = stepStatusList.get(2);
        assertThat(computeSourceDirStatus.getStep(), is(computeSourceDir));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        //assertThat(jobStatus.getRepairSourceDir(), is(new File[] {new File(repoDir.getAbsolutePath()+"/a-module/src/custom/folder"), new File(repoDir.getAbsolutePath()+"/test-projects/src/main/java")}));
    }

    @Test
    public void testComputeSourceDirWithReflexiveReferences() throws IOException {
        long buildId = 345990212;
        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computesourcedirOverflow");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getRepoSlug()).thenReturn(toBeInspected.getBuggyBuild().getRepository().getSlug());
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
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, true, false);

        cloneStep.setNextStep(new CheckoutBuggyBuild(inspector, true)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.isShouldStop(), is(true));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus computeSourceDirStatus = stepStatusList.get(2);
        assertThat(computeSourceDirStatus.getStep(), is(computeSourceDir));
        assertThat(computeSourceDirStatus.isSuccess(), is(false));

        for (StepStatus stepStatus : stepStatusList) {
            if (stepStatus.getStep() != computeSourceDir) {
                assertThat(stepStatus.isSuccess(), is(true));
            }
        }

    }
}

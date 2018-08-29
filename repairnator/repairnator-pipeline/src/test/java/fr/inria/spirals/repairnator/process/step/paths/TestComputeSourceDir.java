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
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutPatchedBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutType;
import fr.inria.spirals.repairnator.process.utils4tests.ProjectInspectorMocker;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
public class TestComputeSourceDir {

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
    public void testComputeSourceDir() throws IOException {
        long buildId = 207924136; // surli/failingProject build

        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_computesourcedir").toFile();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected, CheckoutType.CHECKOUT_BUGGY_BUILD);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, true, false);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true))
                .addNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.isShouldStop(), is(false));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus computeSourceDirStatus = stepStatusList.get(2);
        assertThat(computeSourceDirStatus.getStep(), is(computeSourceDir));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {new File(repoDir.getAbsolutePath()+"/src/main/java").getCanonicalFile()}));
    }

    @Test
    public void testComputeSourceDirWithMultiModuleProject() throws IOException {
        long buildId = 225251586; // Spirals-Team/librepair build

        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_computesourcedir2").toFile();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath()+"/test-projects");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, true, false);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true))
                .addNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.isShouldStop(), is(false));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus computeSourceDirStatus = stepStatusList.get(2);
        assertThat(computeSourceDirStatus.getStep(), is(computeSourceDir));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {new File(repoDir.getAbsolutePath()+"/test-projects/src/main/java").getCanonicalFile()}));
    }

    @Test
    public void testComputeSourceDirWithMultiModuleProject2() throws IOException {
        long buildId = 225251586; // Spirals-Team/librepair build

        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_computesourcedir2").toFile();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath()+"/a-module");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, true, false);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true)).addNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.isShouldStop(), is(false));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus computeSourceDirStatus = stepStatusList.get(2);
        assertThat(computeSourceDirStatus.getStep(), is(computeSourceDir));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {new File(repoDir.getAbsolutePath()+"/a-module/src/custom/folder").getCanonicalFile()}));
    }

    @Test
    public void testComputeSourceDirWithMultiModuleProject3() throws IOException {
        long buildId = 225251586; // Spirals-Team/librepair build

        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_computesourcedir2").toFile();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, true, false);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true)).addNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.isShouldStop(), is(false));
        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(3));
        StepStatus computeSourceDirStatus = stepStatusList.get(2);
        assertThat(computeSourceDirStatus.getStep(), is(computeSourceDir));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {new File(repoDir.getAbsolutePath()+"/a-module/src/custom/folder").getCanonicalFile(), new File(repoDir.getAbsolutePath()+"/test-projects/src/main/java").getCanonicalFile()}));
    }

    @Test
    public void testComputeSourceDirWithMultiModuleProject4() throws IOException {
        long buildId = 216674182; // pac4j/pac4j
        long patchedBuildId = 218753299;

        Build build = this.checkBuildAndReturn(buildId, false);
        Build patchedBuild = this.checkBuildAndReturn(patchedBuildId, false);

        tmpDir = Files.createTempDirectory("test_computesourcedir3").toFile();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, patchedBuild, ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES, "");


        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, true, false);

        cloneStep.addNextStep(new CheckoutPatchedBuild(inspector, true)).addNextStep(computeSourceDir);
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
                new File(repoDir.getAbsolutePath()+"/pac4j-cas/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/pac4j-config/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/pac4j-core/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/pac4j-gae/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/pac4j-http/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/pac4j-jwt/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/pac4j-ldap/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/pac4j-mongo/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/pac4j-oauth/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/pac4j-oidc/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/pac4j-openid/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/pac4j-saml/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/pac4j-sql/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/pac4j-stormpath/src/main/java").getCanonicalFile()
        }));
    }

    // fixme: the test is not passing anymore when executing the whole test suite
    @Ignore
    @Test
    public void testComputeSourceDirWithReflexiveReferences() throws IOException {
        long buildId = 345990212;

        Build build = this.checkBuildAndReturn(buildId, true);

        tmpDir = Files.createTempDirectory("test_computesourcedirOverflow").toFile();

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");


        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        jobStatus.setFailingModulePath(repoDir.getAbsolutePath());

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector, true, false);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true)).addNextStep(computeSourceDir);
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

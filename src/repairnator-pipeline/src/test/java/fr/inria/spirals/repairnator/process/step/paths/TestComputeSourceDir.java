package fr.inria.spirals.repairnator.process.step.paths;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.utils.Utils;
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
        RepairnatorConfig.getInstance().setJTravisEndpoint("https://api.travis-ci.com");
    }

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
        FileHelper.deleteFile(tmpDir);
    }

    @Test
    public void testComputeSourceDir() throws IOException {
        long buildId = 224246334; // repairnator/failingProject -> master

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
        long buildId = 224264992; // repairnator/failingProject -> multi-module

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

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {
                new File(repoDir.getAbsolutePath()+"/src/maven-repair/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/src/repairnator-core/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/src/repairnator-jenkins-plugin/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/src/repairnator-pipeline/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/src/repairnator-realtime/src/main/java").getCanonicalFile(),
        }));
    }

    @Test
    public void testComputeSourceDirWithMultiModuleProject2() throws IOException {
        long buildId = 224120644; // hs-web/hsweb-framework build

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

        assertThat(jobStatus.getRepairSourceDir(), is(new File[] {
                new File(repoDir.getAbsolutePath()+"/hsweb-authorization/hsweb-authorization-api/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-authorization/hsweb-authorization-basic/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-authorization/hsweb-authorization-oauth2/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-commons/hsweb-commons-api/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-commons/hsweb-commons-crud/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-concurrent/hsweb-concurrent-cache/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-core/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-datasource/hsweb-datasource-api/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-datasource/hsweb-datasource-jta/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-datasource/hsweb-datasource-web/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-logging/hsweb-access-logging-aop/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-logging/hsweb-access-logging-api/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-starter/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-system/hsweb-system-authorization/hsweb-system-authorization-api/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-system/hsweb-system-authorization/hsweb-system-authorization-default/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-system/hsweb-system-authorization/hsweb-system-authorization-oauth2/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-system/hsweb-system-dictionary/src/main/java").getCanonicalFile(),
                new File(repoDir.getAbsolutePath()+"/hsweb-system/hsweb-system-file/src/main/java").getCanonicalFile()
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

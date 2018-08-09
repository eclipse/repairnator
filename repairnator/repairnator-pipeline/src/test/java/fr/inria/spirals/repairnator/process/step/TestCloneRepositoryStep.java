package fr.inria.spirals.repairnator.process.step;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.files.FileHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.utils4tests.ProjectInspectorMocker;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
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
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by urli on 21/02/2017.
 */
public class TestCloneRepositoryStep {

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
    public void testCloneMasterBuild() throws IOException {
        long buildId = 207924136; // surli/failingProject build

        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_clone").toFile();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);
        cloneStep.execute();

        assertThat(cloneStep.isShouldStop(), is(false));

        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(1));
        StepStatus cloneStatus = stepStatusList.get(0);
        assertThat(cloneStatus.getStep(), is(cloneStep));
        assertThat(cloneStatus.isSuccess(), is(true));

        Git gitDir = Git.open(new File(tmpDir, "repo"));
        Ref ref = gitDir.getRepository().exactRef("HEAD");

        assertThat(ref.isSymbolic(), is(true));

        ref = ref.getTarget();

        assertThat(ref.getObjectId().getName(), not(build.getCommit().getSha())); // no check out yet
    }

    @Test
    public void testCloneBuildWithSubmodule() throws IOException {
        long buildId = 410212137; // surli/failingProject build

        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_clone").toFile();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        AbstractStep cloneStep = new CloneRepository(inspector).addNextStep(new CheckoutBuggyBuild(inspector, true));

        cloneStep.execute();

        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(2));
        StepStatus cloneStatus = stepStatusList.get(0);
        assertThat(cloneStatus.getStep(), is(cloneStep));
        assertThat(cloneStatus.isSuccess(), is(true));

        File readmeInSubmodule = new File(tmpDir, "repo/test-repairnator-bears/README.md");
        assertThat("Submodule are not supported", readmeInSubmodule.exists(), is(true));
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

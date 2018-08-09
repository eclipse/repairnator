package fr.inria.spirals.repairnator.process.step.checkoutrepository;

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
import fr.inria.spirals.repairnator.process.utils4tests.ProjectInspectorMocker;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by urli on 06/03/2017.
 */
public class TestCheckoutBuild {

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
    public void testCheckoutBuggyBuild() throws IOException, GitAPIException {
        long buildId = 207924136; // surli/failingProject build

        RepairnatorConfig repairnatorConfig = RepairnatorConfig.getInstance();
        repairnatorConfig.setClean(false);

        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_checkout").toFile();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutBuggyBuild checkoutBuggyBuild = new CheckoutBuggyBuild(inspector, true);

        cloneStep.addNextStep(checkoutBuggyBuild);
        cloneStep.execute();

        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(2));
        StepStatus checkoutStatus = stepStatusList.get(1);
        assertThat(checkoutStatus.getStep(), is(checkoutBuggyBuild));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertThat(checkoutBuggyBuild.isShouldStop(), is(false));

        Git gitDir = Git.open(new File(tmpDir, "repo"));
        Iterable<RevCommit> logs = gitDir.log().call();

        Iterator<RevCommit> iterator = logs.iterator();
        boolean foundRightCommitAfterRepairCommits = false;
        boolean stopSearch = false;

        while (iterator.hasNext() && !stopSearch) {
            RevCommit revCommit = iterator.next();

            if (revCommit.getName().equals(build.getCommit().getSha())) {
                foundRightCommitAfterRepairCommits = true;
                stopSearch = true;
            } else if (!revCommit.getShortMessage().contains("repairnator")) {
                stopSearch = true;
            }
        }

        assertThat(foundRightCommitAfterRepairCommits, is(true));
    }

    @Test
    public void testCheckoutBuildFromPRWithMissingMerge() throws IOException {
        long buildId = 199527447; // surli/failingProject build

        Build build = this.checkBuildAndReturn(buildId, true);

        tmpDir = Files.createTempDirectory("test_checkout").toFile();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutBuggyBuild checkoutBuggyBuild = new CheckoutBuggyBuild(inspector, true);

        cloneStep.addNextStep(checkoutBuggyBuild);
        cloneStep.execute();

        assertThat(checkoutBuggyBuild.isShouldStop(), is(true));

        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(2));
        StepStatus cloneStatus = stepStatusList.get(0);
        assertThat(cloneStatus.getStep(), is(cloneStep));
        assertThat(cloneStatus.isSuccess(), is(true));


        StepStatus checkoutStatus = stepStatusList.get(1);
        assertThat(checkoutStatus.getStep(), is(checkoutBuggyBuild));
        assertThat(checkoutStatus.isSuccess(), is(false));
        assertThat(checkoutStatus.getDiagnostic(), is(PipelineState.BUILDNOTCHECKEDOUT.name()));

        String serializedStatus = AbstractDataSerializer.getPrettyPrintState(inspector);
        assertThat(serializedStatus, is(PipelineState.BUILDNOTCHECKEDOUT.name()));
    }

    @Test
    public void testCheckoutBuildFromPRWithMerge() throws IOException {
        long buildId = 199923736; // surli/failingProject build

        Build build = this.checkBuildAndReturn(buildId, true);

        tmpDir = Files.createTempDirectory("test_checkout").toFile();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutBuggyBuild checkoutBuggyBuild = new CheckoutBuggyBuild(inspector, true);

        cloneStep.addNextStep(checkoutBuggyBuild);
        cloneStep.execute();

        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(2));
        StepStatus checkoutStatus = stepStatusList.get(1);
        assertThat(checkoutStatus.getStep(), is(checkoutBuggyBuild));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertThat(checkoutBuggyBuild.isShouldStop(), is(false));
    }

    @Test
    public void testCheckoutBuildFromPROtherRepo() throws IOException {
        long buildId = 196568333; // surli/failingProject build

        Build build = this.checkBuildAndReturn(buildId, true);

        tmpDir = Files.createTempDirectory("test_checkout").toFile();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutBuggyBuild checkoutBuggyBuild = new CheckoutBuggyBuild(inspector, true);

        cloneStep.addNextStep(checkoutBuggyBuild);
        cloneStep.execute();

        // cannot get the PR information so it stop now
        assertThat(checkoutBuggyBuild.isShouldStop(), is(true));

        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(2));
        StepStatus cloneStatus = stepStatusList.get(0);
        assertThat(cloneStatus.getStep(), is(cloneStep));
        assertThat(cloneStatus.isSuccess(), is(true));


        StepStatus checkoutStatus = stepStatusList.get(1);
        assertThat(checkoutStatus.getStep(), is(checkoutBuggyBuild));
        assertThat(checkoutStatus.isSuccess(), is(false));
        assertThat(checkoutStatus.getDiagnostic(), is(PipelineState.BUILDNOTCHECKEDOUT.name()));

        String serializedStatus = AbstractDataSerializer.getPrettyPrintState(inspector);
        assertThat(serializedStatus, is(PipelineState.BUILDNOTCHECKEDOUT.name()));
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

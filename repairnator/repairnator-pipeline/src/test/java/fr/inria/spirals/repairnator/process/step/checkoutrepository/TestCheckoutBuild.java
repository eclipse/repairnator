package fr.inria.spirals.repairnator.process.step.checkoutrepository;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.config.RepairnatorConfigException;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by urli on 06/03/2017.
 */
public class TestCheckoutBuild {

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.ERROR);
    }

    @After
    public void tearDown() {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void testCheckoutBuggyBuild() throws IOException, GitAPIException, RepairnatorConfigException {
        long buildId = 207924136; // surli/failingProject build

        RepairnatorConfig repairnatorConfig = RepairnatorConfig.getInstance();
        repairnatorConfig.setClean(false);

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_checkout");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getRepoSlug()).thenReturn(toBeInspected.getBuggyBuild().getRepository().getSlug());
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutBuggyBuild checkoutBuggyBuild = new CheckoutBuggyBuild(inspector, true);

        cloneStep.setNextStep(checkoutBuggyBuild);
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
    public void testCheckoutBuildFromPRWithMissingMerge() throws IOException, GitAPIException {
        long buildId = 199527447; // surli/failingProject build

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_checkout");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getRepoSlug()).thenReturn(toBeInspected.getBuggyBuild().getRepository().getSlug());
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutBuggyBuild checkoutBuggyBuild = new CheckoutBuggyBuild(inspector, true);

        cloneStep.setNextStep(checkoutBuggyBuild);
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
    public void testCheckoutBuildFromPRWithMerge() throws IOException, GitAPIException {
        long buildId = 199923736; // surli/failingProject build

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_checkout");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getRepoSlug()).thenReturn(toBeInspected.getBuggyBuild().getRepository().getSlug());
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutBuggyBuild checkoutBuggyBuild = new CheckoutBuggyBuild(inspector, true);

        cloneStep.setNextStep(checkoutBuggyBuild);
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
    public void testCheckoutBuildFromPROtherRepo() throws IOException, GitAPIException {
        long buildId = 196568333; // surli/failingProject build

        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());
        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_checkout");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getRepoSlug()).thenReturn(toBeInspected.getBuggyBuild().getRepository().getSlug());
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuggyBuild()).thenReturn(build);
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutBuggyBuild checkoutBuggyBuild = new CheckoutBuggyBuild(inspector, true);

        cloneStep.setNextStep(checkoutBuggyBuild);
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
}

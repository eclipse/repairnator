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
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.hamcrest.core.Is;
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
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by urli on 21/04/2017.
 */
public class TestCheckoutBuggyBuildSourceCode {

    private File tmpDir;

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.ERROR);
        RepairnatorConfig.getInstance().setGithubUserEmail("noreply@github.com");
        RepairnatorConfig.getInstance().setGithubUserName("repairnator");
    }

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
        FileHelper.deleteFile(tmpDir);
    }

    @Test
    public void testCheckoutPreviousBuildSourceCodeNoPR() throws IOException, GitAPIException {
        long buildId = 221992429; // INRIA/spoon
        long previousBuildId = 218213030;
        ScannedBuildStatus status = ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES;

        Build build = this.checkBuildAndReturn(buildId, false);
        Build previousBuild = this.checkBuildAndReturn(previousBuildId, false);

        tmpDir = Files.createTempDirectory("test_checkoutprevious").toFile();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(previousBuild, build, status, "");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutBuggyBuildSourceCode checkoutBuild = new CheckoutBuggyBuildSourceCode(inspector, true);

        cloneStep.addNextStep(checkoutBuild);
        cloneStep.execute();

        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(2));
        StepStatus checkoutStatus = stepStatusList.get(1);
        assertThat(checkoutStatus.getStep(), is(checkoutBuild));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertThat(checkoutBuild.isShouldStop(), is(false));

        Git gitDir = Git.open(new File(tmpDir, "repo"));

        Iterable<RevCommit> logs = gitDir.log().call();

        Iterator<RevCommit> iterator = logs.iterator();
        boolean foundRightCommitAfterRepairCommits = false;
        boolean foundUndoSourceCodeCommit = false;
        boolean stopSearch = false;

        while (iterator.hasNext() && !stopSearch) {
            RevCommit revCommit = iterator.next();

            if (revCommit.getShortMessage().equals("Undo changes on source code")) {
                foundUndoSourceCodeCommit = true;
                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                ObjectReader reader = gitDir.getRepository().newObjectReader();
                RevCommit prevCommit = iterator.next();
                oldTreeIter.reset(reader, prevCommit.getTree());
                newTreeIter.reset(reader, revCommit.getTree());

                List<DiffEntry> diff = gitDir.diff().setOldTree(oldTreeIter).setNewTree(newTreeIter).call();

                for (DiffEntry entry : diff) {
                    assertThat(entry.getOldPath(), startsWith("src/main/java/"));
                }

                revCommit = prevCommit;
            }

            if (revCommit.getName().equals(build.getCommit().getSha())) {
                foundRightCommitAfterRepairCommits = true;
            }

            if (!revCommit.getShortMessage().contains("repairnator")) {
                stopSearch = true;
            }

            if (foundRightCommitAfterRepairCommits && foundUndoSourceCodeCommit) {
                stopSearch = true;
            }
        }

        assertThat(foundRightCommitAfterRepairCommits, is(true));
        assertThat(foundUndoSourceCodeCommit, is(true));
    }

    @Test
    public void testCheckoutPreviousBuildSourceCodeNoPR2() throws IOException, GitAPIException {
        long buildId = 222020421; // alibaba/fastjson
        long previousBuildId = 222016611;
        ScannedBuildStatus status = ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES;

        Build build = this.checkBuildAndReturn(buildId, false);
        Build previousBuild = this.checkBuildAndReturn(previousBuildId, false);

        tmpDir = Files.createTempDirectory("test_checkoutprevious").toFile();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(previousBuild, build, status, "");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutBuggyBuildSourceCode checkoutBuild = new CheckoutBuggyBuildSourceCode(inspector, true);

        cloneStep.addNextStep(checkoutBuild);
        cloneStep.execute();

        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(2));
        StepStatus checkoutStatus = stepStatusList.get(1);
        assertThat(checkoutStatus.getStep(), is(checkoutBuild));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertThat(checkoutBuild.isShouldStop(), is(false));

        Git gitDir = Git.open(new File(tmpDir, "repo"));

        Iterable<RevCommit> logs = gitDir.log().call();

        Iterator<RevCommit> iterator = logs.iterator();
        boolean foundRightCommitAfterRepairCommits = false;
        boolean foundUndoSourceCodeCommit = false;
        boolean stopSearch = false;

        while (iterator.hasNext() && !stopSearch) {
            RevCommit revCommit = iterator.next();

            if (revCommit.getShortMessage().equals("Undo changes on source code")) {
                foundUndoSourceCodeCommit = true;
                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                ObjectReader reader = gitDir.getRepository().newObjectReader();
                RevCommit prevCommit = iterator.next();
                oldTreeIter.reset(reader, prevCommit.getTree());
                newTreeIter.reset(reader, revCommit.getTree());

                List<DiffEntry> diff = gitDir.diff().setOldTree(oldTreeIter).setNewTree(newTreeIter).call();

                for (DiffEntry entry : diff) {
                    assertThat(entry.getOldPath(), startsWith("src/main/java/"));
                }

                revCommit = prevCommit;
            }

            if (revCommit.getName().equals(build.getCommit().getSha())) {
                foundRightCommitAfterRepairCommits = true;
            }

            if (!revCommit.getShortMessage().contains("repairnator")) {
                stopSearch = true;
            }

            if (foundRightCommitAfterRepairCommits && foundUndoSourceCodeCommit) {
                stopSearch = true;
            }
        }

        assertThat(foundRightCommitAfterRepairCommits, is(true));
        assertThat(foundUndoSourceCodeCommit, is(true));
    }

    @Test
    public void testCheckoutPreviousBuildSourceCodeWithPR() throws IOException, GitAPIException {
        long buildId = 223248816; // HubSpot/Singularity
        long previousBuildId = 222209171;
        ScannedBuildStatus status = ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES;

        Build build = this.checkBuildAndReturn(buildId, true);
        Build previousBuild = this.checkBuildAndReturn(previousBuildId, true);

        tmpDir = Files.createTempDirectory("test_checkoutprevious").toFile();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(previousBuild, build, status, "");

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");

        ProjectInspector inspector = ProjectInspectorMocker.mockProjectInspector(jobStatus, tmpDir, toBeInspected);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutBuggyBuildSourceCode checkoutBuild = new CheckoutBuggyBuildSourceCode(inspector, true);

        cloneStep.addNextStep(checkoutBuild);
        cloneStep.execute();

        List<StepStatus> stepStatusList = jobStatus.getStepStatuses();
        assertThat(stepStatusList.size(), is(2));
        StepStatus checkoutStatus = stepStatusList.get(1);
        assertThat(checkoutStatus.getStep(), is(checkoutBuild));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        assertThat(checkoutBuild.isShouldStop(), is(false));

        Git gitDir = Git.open(new File(tmpDir, "repo"));

        Iterable<RevCommit> logs = gitDir.log().call();

        Iterator<RevCommit> iterator = logs.iterator();
        boolean foundRightCommitAfterRepairCommits = false;
        boolean foundUndoSourceCodeCommit = false;
        boolean stopSearch = false;

        while (iterator.hasNext() && !stopSearch) {
            RevCommit revCommit = iterator.next();

            System.out.println(revCommit.getShortMessage());
            if (revCommit.getShortMessage().equals("Undo changes on source code")) {
                foundUndoSourceCodeCommit = true;
                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                ObjectReader reader = gitDir.getRepository().newObjectReader();
                RevCommit prevCommit = iterator.next();
                oldTreeIter.reset(reader, prevCommit.getTree());
                newTreeIter.reset(reader, revCommit.getTree());

                List<DiffEntry> diff = gitDir.diff().setOldTree(oldTreeIter).setNewTree(newTreeIter).call();

                for (DiffEntry entry : diff) {
                    assertThat(entry.getOldPath(), startsWith("src/main/java/"));
                }

                revCommit = prevCommit;
            }

            if (!revCommit.getShortMessage().contains("repairnator") && !revCommit.getShortMessage().contains("merge")) {
                stopSearch = true;
            }
        }

        assertThat(foundUndoSourceCodeCommit, is(true));
    }

    private Build checkBuildAndReturn(long buildId, boolean isPR) {
        Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
        assertTrue(optionalBuild.isPresent());

        Build build = optionalBuild.get();
        assertThat(build, notNullValue());
        assertThat(buildId, Is.is(build.getId()));
        assertThat(build.isPullRequest(), Is.is(isPR));

        return build;
    }
}

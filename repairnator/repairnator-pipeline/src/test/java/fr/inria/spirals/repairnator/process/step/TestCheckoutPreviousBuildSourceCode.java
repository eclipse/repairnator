package fr.inria.spirals.repairnator.process.step;

import ch.qos.logback.classic.Level;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.ScannedBuildStatus;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.config.RepairnatorConfigException;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutPreviousBuildSourceCode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by urli on 21/04/2017.
 */
public class TestCheckoutPreviousBuildSourceCode {
    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.ERROR);
    }

    @After
    public void tearDown() {
        RepairnatorConfig.deleteInstance();
    }

    @Test
    public void testCheckoutPreviousBuildSourceCodeNoPR() throws IOException, GitAPIException, RepairnatorConfigException {
        int buildId = 221992429; // INRIA/spoon
        int previousBuildId = 218213030;
        ScannedBuildStatus status = ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES;

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));
        assertThat(build.isPullRequest(), is(false));

        Build previousBuild = BuildHelper.getBuildFromId(previousBuildId, null);
        assertThat(previousBuild, notNullValue());
        assertThat(previousBuild.getId(), is(previousBuildId));
        assertThat(previousBuild.isPullRequest(), is(false));

        Path tmpDirPath = Files.createTempDirectory("test_checkoutprevious");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, previousBuild, status, "");

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getPreviousBuild()).thenReturn(previousBuild);
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutPreviousBuildSourceCode checkoutBuild = new CheckoutPreviousBuildSourceCode(inspector);

        cloneStep.setNextStep(checkoutBuild);
        cloneStep.execute();

        assertThat(checkoutBuild.getState(), is(ProjectState.PREVIOUSBUILDCODECHECKEDOUT));
        assertThat(jobStatus.getState(), is(ProjectState.PREVIOUSBUILDCODECHECKEDOUT));

        assertThat(checkoutBuild.shouldStop, is(false));

        Git gitDir = Git.open(new File(tmpDir, "repo"));
        assertThat(gitDir.status().call().isClean(), is(true));

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
    public void testCheckoutPreviousBuildSourceCodeNoPR2() throws IOException, GitAPIException, RepairnatorConfigException {
        int buildId = 222020421; // alibaba/fastjson
        int previousBuildId = 222016611;
        ScannedBuildStatus status = ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES;

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));
        assertThat(build.isPullRequest(), is(false));

        Build previousBuild = BuildHelper.getBuildFromId(previousBuildId, null);
        assertThat(previousBuild, notNullValue());
        assertThat(previousBuild.getId(), is(previousBuildId));
        assertThat(previousBuild.isPullRequest(), is(false));

        Path tmpDirPath = Files.createTempDirectory("test_checkoutprevious");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, previousBuild, status, "");

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getPreviousBuild()).thenReturn(previousBuild);
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutPreviousBuildSourceCode checkoutBuild = new CheckoutPreviousBuildSourceCode(inspector);

        cloneStep.setNextStep(checkoutBuild);
        cloneStep.execute();

        assertThat(checkoutBuild.getState(), is(ProjectState.PREVIOUSBUILDCODECHECKEDOUT));
        assertThat(jobStatus.getState(), is(ProjectState.PREVIOUSBUILDCODECHECKEDOUT));

        assertThat(checkoutBuild.shouldStop, is(false));

        Git gitDir = Git.open(new File(tmpDir, "repo"));
        assertThat(gitDir.status().call().isClean(), is(true));

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
    public void testCheckoutPreviousBuildSourceCodeWithPR() throws IOException, GitAPIException, RepairnatorConfigException {
        int buildId = 223248816; // HubSpot/Singularity
        int previousBuildId = 222209171;
        ScannedBuildStatus status = ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES;

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));
        assertThat(build.isPullRequest(), is(true));

        Build previousBuild = BuildHelper.getBuildFromId(previousBuildId, null);
        assertThat(previousBuild, notNullValue());
        assertThat(previousBuild.getId(), is(previousBuildId));
        assertThat(previousBuild.isPullRequest(), is(true));

        Path tmpDirPath = Files.createTempDirectory("test_checkoutprevious");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, previousBuild, status, "");

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getPreviousBuild()).thenReturn(previousBuild);
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutPreviousBuildSourceCode checkoutBuild = new CheckoutPreviousBuildSourceCode(inspector);

        cloneStep.setNextStep(checkoutBuild);
        cloneStep.execute();

        assertThat(checkoutBuild.getState(), is(ProjectState.PREVIOUSBUILDCODECHECKEDOUT));
        assertThat(jobStatus.getState(), is(ProjectState.PREVIOUSBUILDCODECHECKEDOUT));

        assertThat(checkoutBuild.shouldStop, is(false));

        Git gitDir = Git.open(new File(tmpDir, "repo"));
        assertThat(gitDir.status().call().isClean(), is(true));

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
}

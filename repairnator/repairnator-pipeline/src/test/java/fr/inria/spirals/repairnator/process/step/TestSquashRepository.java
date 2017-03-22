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
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by urli on 22/03/2017.
 */
public class TestSquashRepository {

    static {
        Utils.setLoggersLevel(Level.ERROR);
    }

    @Test
    public void testSquashRepositoryOnSmallRepoWillNotSquashIt() throws IOException, GitAPIException {
        int buildId = 207924136; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_squash");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        SquashRepository squashStep = new SquashRepository(inspector);
        CloneRepository cloneStep = new CloneRepository(inspector);
        cloneStep.setNextStep(squashStep);

        RepairnatorConfig config = cloneStep.getConfig();
        config.setPush(true);

        cloneStep.execute();

        verify(inspector, times(1)).setState(ProjectState.NOT_SQUASHED_REPO);
        Status status = Git.open(new File(tmpDir, "repo")).status().call();
        assertThat(status.isClean(), is(true));
    }

    @Test
    public void testSquashRepositoryOnProjectWhichChangeFileAtBuildWorks() throws IOException, GitAPIException {
        int buildId = 212649623; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_squash2");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        SquashRepository squashStep = new SquashRepository(inspector);
        CloneRepository cloneStep = new CloneRepository(inspector);
        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(new BuildProject(inspector)).setNextStep(squashStep);

        RepairnatorConfig config = cloneStep.getConfig();
        config.setPush(true);

        cloneStep.execute();

        verify(inspector, times(1)).setState(ProjectState.SQUASHED_REPO);

        File repo = new File(tmpDir, "repo");

        List<String> listFiles = Arrays.asList(repo.list());

        assertThat(listFiles.contains("repairnator.properties"), is(true));
        assertThat(listFiles.contains("repairnator.maven.buildproject.log"), is(true));

        Status status = Git.open(repo).status().call();
        assertThat(status.isClean(), is(true));
    }

    @Test
    public void testSquashRepositoryOnProjectWithHundredCommits() throws IOException, GitAPIException {
        int buildId = 209082903; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_squash3");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        SquashRepository squashStep = new SquashRepository(inspector);
        CloneRepository cloneStep = new CloneRepository(inspector);
        cloneStep.setNextStep(squashStep);

        RepairnatorConfig config = cloneStep.getConfig();
        config.setPush(true);

        cloneStep.execute();

        verify(inspector, times(1)).setState(ProjectState.SQUASHED_REPO);
        Status status = Git.open(new File(tmpDir, "repo")).status().call();
        assertThat(status.isClean(), is(true));
    }
}

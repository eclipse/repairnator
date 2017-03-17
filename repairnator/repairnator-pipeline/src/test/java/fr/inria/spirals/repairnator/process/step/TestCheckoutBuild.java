package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.ScannedBuildStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by urli on 06/03/2017.
 */
public class TestCheckoutBuild {

    @Test
    public void testCheckoutBuild() throws IOException, GitAPIException {
        int buildId = 207924136; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_checkout");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutBuild checkoutBuild = new CheckoutBuild(inspector);

        cloneStep.setNextStep(checkoutBuild);
        cloneStep.execute();

        assertThat(checkoutBuild.getState(), is(ProjectState.BUILDCHECKEDOUT));
        verify(inspector, times(1)).setState(ProjectState.BUILDCHECKEDOUT);

        assertThat(checkoutBuild.shouldStop, is(false));

        Git gitDir = Git.open(tmpDir);
        Iterable<RevCommit> logs = gitDir.log().call();

        Iterator<RevCommit> iterator = logs.iterator();

        assertThat(iterator.hasNext(), is(true));

        assertThat(iterator.next().getName(), is(build.getCommit().getSha()));
    }

    @Test
    public void testCheckoutBuildFromPR() throws IOException, GitAPIException {
        int buildId = 199527447; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_checkout");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);

        CloneRepository cloneStep = new CloneRepository(inspector);
        CheckoutBuild checkoutBuild = new CheckoutBuild(inspector);

        cloneStep.setNextStep(checkoutBuild);
        cloneStep.execute();

        assertThat(checkoutBuild.getState(), is(ProjectState.BUILDCHECKEDOUT));
        verify(inspector, times(1)).setState(ProjectState.BUILDCHECKEDOUT);

        assertThat(checkoutBuild.shouldStop, is(false));
    }
}

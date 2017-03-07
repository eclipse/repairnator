package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.ScannedBuildStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by urli on 07/03/2017.
 */
public class TestTestProject {

    @Test
    public void testTestProjectWhenFailing() throws IOException {
        int buildId = 207890790; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_testproject");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");

        CloneRepository cloneStep = new CloneRepository(inspector);
        TestProject testProject = new TestProject(inspector);


        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(testProject);
        cloneStep.execute();

        assertThat(testProject.shouldStop, is(false));
        assertThat(testProject.getState(), is(ProjectState.TESTABLE));
        verify(inspector, times(1)).setState(ProjectState.TESTABLE);
    }

    @Test
    public void testTestProjectWhenErroring() throws IOException {
        int buildId = 208240908; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_clone");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");

        CloneRepository cloneStep = new CloneRepository(inspector);
        TestProject testProject = new TestProject(inspector);


        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(testProject);
        cloneStep.execute();

        assertThat(testProject.shouldStop, is(false));
        assertThat(testProject.getState(), is(ProjectState.TESTABLE));
        verify(inspector, times(1)).setState(ProjectState.TESTABLE);
    }

    @Test
    public void testTestProjectWhenNotFailing() throws IOException {
        int buildId = 201176013; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_clone");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");

        CloneRepository cloneStep = new CloneRepository(inspector);
        TestProject testProject = new TestProject(inspector);


        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(testProject);
        cloneStep.execute();

        assertThat(testProject.shouldStop, is(false));
        assertThat(testProject.getState(), is(ProjectState.NOTFAILING));
        verify(inspector, times(1)).setState(ProjectState.NOTFAILING);
    }
}

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
public class TestComputeSourceDir {

    @Test
    public void testComputeSourceDir() throws IOException {
        int buildId = 201176013; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_computesourcedir");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL, "");

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");

        GatherTestInformation mockGathertest = mock(GatherTestInformation.class);
        when(mockGathertest.getFailingModulePath()).thenReturn(tmpDir.getAbsolutePath());

        when(inspector.getTestInformations()).thenReturn(mockGathertest);

        CloneRepository cloneStep = new CloneRepository(inspector);
        ComputeSourceDir computeSourceDir = new ComputeSourceDir(inspector);

        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(new TestProject(inspector)).setNextStep(computeSourceDir);
        cloneStep.execute();

        assertThat(computeSourceDir.shouldStop, is(false));
        assertThat(computeSourceDir.getState(), is(ProjectState.SOURCEDIRCOMPUTED));
        verify(inspector, times(1)).setState(ProjectState.SOURCEDIRCOMPUTED);

        verify(inspector, times(1)).setRepairSourceDir(new File[] {new File(tmpDir.getAbsolutePath()+"/src/main/java")});
    }
}

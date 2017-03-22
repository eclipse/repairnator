package fr.inria.spirals.repairnator.process.step;

import ch.qos.logback.classic.Level;
import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.TestsInformation;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.ScannedBuildStatus;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.gatherinfocontract.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfocontract.BuildShouldPass;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import fr.inria.spirals.repairnator.process.testinformation.FailureType;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by urli on 07/03/2017.
 */
public class TestGatherTestInformation {

    static {
        Utils.setLoggersLevel(Level.ERROR);
    }

    @Test
    public void testGatherTestInformationWhenFailing() throws IOException {
        int buildId = 207890790; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_gathertest");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");

        CloneRepository cloneStep = new CloneRepository(inspector);
        GatherTestInformation gatherTestInformation = new GatherTestInformation(inspector, new BuildShouldFail());


        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(new TestProject(inspector)).setNextStep(gatherTestInformation);
        cloneStep.execute();

        assertThat(gatherTestInformation.shouldStop, is(false));
        assertThat(gatherTestInformation.getState(), is(ProjectState.HASTESTFAILURE));
        verify(inspector, times(1)).setState(ProjectState.HASTESTFAILURE);

        assertThat(gatherTestInformation.getFailingModulePath(), is(tmpDir.getCanonicalPath()));
        assertThat(gatherTestInformation.getNbTotalTests(), is(98));
        assertThat(gatherTestInformation.getNbFailingTests(), is(26));
        assertThat(gatherTestInformation.getNbErroringTests(), is(5));
        assertThat(gatherTestInformation.getNbSkippingTests(), is(0));

        Set<String> failureNames = gatherTestInformation.getFailureNames();

        assertThat(failureNames.contains("java.lang.StringIndexOutOfBoundsException"), is(true));
        assertThat(failureNames.size(), is(5));

        assertThat(gatherTestInformation.getFailureLocations().size(), is(10));
    }

    @Test
    public void testGatherTestInformationOnlyOneErroring() throws IOException {
        int buildId = 208897371; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_gathertest");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");

        CloneRepository cloneStep = new CloneRepository(inspector);
        GatherTestInformation gatherTestInformation = new GatherTestInformation(inspector, new BuildShouldFail());


        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(new TestProject(inspector)).setNextStep(gatherTestInformation);
        cloneStep.execute();

        assertThat(gatherTestInformation.shouldStop, is(false));
        assertThat(gatherTestInformation.getState(), is(ProjectState.HASTESTERRORS));
        verify(inspector, times(1)).setState(ProjectState.HASTESTERRORS);

        assertThat(gatherTestInformation.getFailingModulePath(), is(tmpDir.getCanonicalPath()));
        assertThat(gatherTestInformation.getNbTotalTests(), is(8));
        assertThat(gatherTestInformation.getNbFailingTests(), is(0));
        assertThat(gatherTestInformation.getNbErroringTests(), is(1));
        assertThat(gatherTestInformation.getNbSkippingTests(), is(0));

        Set<String> failureNames = gatherTestInformation.getFailureNames();

        assertThat(failureNames.contains("java.lang.StringIndexOutOfBoundsException"), is(true));
        assertThat(failureNames.size(), is(1));

        assertThat(gatherTestInformation.getFailureLocations().size(), is(1));

        FailureLocation expectedFailureLocation = new FailureLocation("nopol_examples.nopol_example_1.NopolExampleTest");
        FailureType failureType = new FailureType("java.lang.StringIndexOutOfBoundsException", "String index out of range: -5", true);
        expectedFailureLocation.addFailure(failureType);
        expectedFailureLocation.addErroringMethod("nopol_examples.nopol_example_1.NopolExampleTest#test5");

        FailureLocation actualLocation = gatherTestInformation.getFailureLocations().iterator().next();

        assertThat(actualLocation, is(expectedFailureLocation));
    }

    @Test
    public void testGatherTestInformationWhenErroring() throws IOException {
        int buildId = 208240908; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_gathertest");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");

        CloneRepository cloneStep = new CloneRepository(inspector);
        GatherTestInformation gatherTestInformation = new GatherTestInformation(inspector, new BuildShouldFail());


        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(new TestProject(inspector)).setNextStep(gatherTestInformation);
        cloneStep.execute();

        assertThat(gatherTestInformation.shouldStop, is(false));
        assertThat(gatherTestInformation.getState(), is(ProjectState.HASTESTERRORS));
        verify(inspector, times(1)).setState(ProjectState.HASTESTERRORS);

        assertThat(gatherTestInformation.getFailingModulePath(), is(tmpDir.getCanonicalPath()));
        assertThat(gatherTestInformation.getNbTotalTests(), is(26));
        assertThat(gatherTestInformation.getNbFailingTests(), is(0));
        assertThat(gatherTestInformation.getNbErroringTests(), is(5));
        assertThat(gatherTestInformation.getNbSkippingTests(), is(0));

        Set<String> failureNames = gatherTestInformation.getFailureNames();

        assertThat(failureNames.contains("java.lang.NullPointerException"), is(true));
        assertThat(failureNames.size(), is(3));

        assertThat(gatherTestInformation.getFailureLocations().size(), is(4));
    }

    @Test
    public void testGatherTestInformationWhenNotFailing() throws IOException {
        int buildId = 201176013; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_gathertest");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");

        CloneRepository cloneStep = new CloneRepository(inspector);
        GatherTestInformation gatherTestInformation = new GatherTestInformation(inspector, new BuildShouldFail());


        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(new TestProject(inspector)).setNextStep(gatherTestInformation);
        cloneStep.execute();

        assertThat(gatherTestInformation.shouldStop, is(true));
        assertThat(gatherTestInformation.getState(), is(ProjectState.NOTFAILING));
        verify(inspector, times(1)).setState(ProjectState.NOTFAILING);

        assertThat(gatherTestInformation.getFailingModulePath(), nullValue());
        assertThat(gatherTestInformation.getNbTotalTests(), is(1));
        assertThat(gatherTestInformation.getNbFailingTests(), is(0));
        assertThat(gatherTestInformation.getNbErroringTests(), is(0));
        assertThat(gatherTestInformation.getNbSkippingTests(), is(0));

        Set<String> failureNames = gatherTestInformation.getFailureNames();
        assertThat(failureNames.size(), is(0));
        assertThat(gatherTestInformation.getFailureLocations().size(), is(0));
    }

    @Test
    public void testGatherTestInformationWhenNotFailingWithPassingContract() throws IOException {
        int buildId = 201176013; // surli/failingProject build

        Build build = BuildHelper.getBuildFromId(buildId, null);
        assertThat(build, notNullValue());
        assertThat(buildId, is(build.getId()));

        Path tmpDirPath = Files.createTempDirectory("test_gathertest");
        File tmpDir = tmpDirPath.toFile();
        tmpDir.deleteOnExit();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL);

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");

        CloneRepository cloneStep = new CloneRepository(inspector);
        GatherTestInformation gatherTestInformation = new GatherTestInformation(inspector, new BuildShouldPass());


        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(new TestProject(inspector)).setNextStep(gatherTestInformation);
        cloneStep.execute();

        assertThat(gatherTestInformation.shouldStop, is(false));
        assertThat(gatherTestInformation.getState(), is(ProjectState.NOTFAILING));
        verify(inspector, times(1)).setState(ProjectState.NOTFAILING);

        assertThat(gatherTestInformation.getFailingModulePath(), nullValue());
        assertThat(gatherTestInformation.getNbTotalTests(), is(1));
        assertThat(gatherTestInformation.getNbFailingTests(), is(0));
        assertThat(gatherTestInformation.getNbErroringTests(), is(0));
        assertThat(gatherTestInformation.getNbSkippingTests(), is(0));

        Set<String> failureNames = gatherTestInformation.getFailureNames();
        assertThat(failureNames.size(), is(0));
        assertThat(gatherTestInformation.getFailureLocations().size(), is(0));
    }
}

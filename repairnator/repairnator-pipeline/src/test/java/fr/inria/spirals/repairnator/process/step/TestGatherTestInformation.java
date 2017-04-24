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
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuild;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldPass;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import fr.inria.spirals.repairnator.process.testinformation.FailureType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by urli on 07/03/2017.
 */
public class TestGatherTestInformation {

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.ERROR);
    }

    @After
    public void tearDown() {
        RepairnatorConfig.deleteInstance();
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
        System.out.println("Dirpath : "+tmpDirPath);

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        GatherTestInformation gatherTestInformation = new GatherTestInformation(inspector, new BuildShouldFail(), false);


        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(new TestProject(inspector)).setNextStep(gatherTestInformation);
        cloneStep.execute();

        assertThat(gatherTestInformation.shouldStop, is(false));
        assertThat(gatherTestInformation.getState(), is(ProjectState.HASTESTFAILURE));
        assertThat(jobStatus.getState(), is(ProjectState.HASTESTFAILURE));

        assertThat(jobStatus.getFailingModulePath(), is(repoDir.getCanonicalPath()));
        assertThat(gatherTestInformation.getNbTotalTests(), is(98));
        assertThat(gatherTestInformation.getNbFailingTests(), is(26));
        assertThat(gatherTestInformation.getNbErroringTests(), is(5));
        assertThat(gatherTestInformation.getNbSkippingTests(), is(0));

        Set<String> failureNames = jobStatus.getFailureNames();

        assertThat(failureNames.contains("java.lang.StringIndexOutOfBoundsException"), is(true));
        assertThat(failureNames.size(), is(5));

        assertThat(jobStatus.getFailureLocations().size(), is(10));
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
        System.out.println("Dirpath : "+tmpDirPath);

        File repoDir = new File(tmpDir, "repo");
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        GatherTestInformation gatherTestInformation = new GatherTestInformation(inspector, new BuildShouldFail(), false);


        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(new TestProject(inspector)).setNextStep(gatherTestInformation);
        cloneStep.execute();

        assertThat(gatherTestInformation.shouldStop, is(false));
        assertThat(gatherTestInformation.getState(), is(ProjectState.HASTESTERRORS));
        assertThat(jobStatus.getState(), is(ProjectState.HASTESTERRORS));

        assertThat(jobStatus.getFailingModulePath(), is(repoDir.getCanonicalPath()));
        assertThat(gatherTestInformation.getNbTotalTests(), is(8));
        assertThat(gatherTestInformation.getNbFailingTests(), is(0));
        assertThat(gatherTestInformation.getNbErroringTests(), is(1));
        assertThat(gatherTestInformation.getNbSkippingTests(), is(0));

        Set<String> failureNames = jobStatus.getFailureNames();

        assertThat(failureNames.contains("java.lang.StringIndexOutOfBoundsException"), is(true));
        assertThat(failureNames.size(), is(1));

        assertThat(jobStatus.getFailureLocations().size(), is(1));

        FailureLocation expectedFailureLocation = new FailureLocation("nopol_examples.nopol_example_1.NopolExampleTest");
        FailureType failureType = new FailureType("java.lang.StringIndexOutOfBoundsException", "String index out of range: -5", true);
        expectedFailureLocation.addFailure(failureType);
        expectedFailureLocation.addErroringMethod("nopol_examples.nopol_example_1.NopolExampleTest#test5");

        FailureLocation actualLocation = jobStatus.getFailureLocations().iterator().next();

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
        System.out.println("Dirpath : "+tmpDirPath);

        File repoDir = new File(tmpDir, "repo");

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL, "");


        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        GatherTestInformation gatherTestInformation = new GatherTestInformation(inspector, new BuildShouldFail(), false);


        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(new TestProject(inspector)).setNextStep(gatherTestInformation);
        cloneStep.execute();

        assertThat(gatherTestInformation.shouldStop, is(false));
        assertThat(gatherTestInformation.getState(), is(ProjectState.HASTESTERRORS));
        assertThat(jobStatus.getState(), is(ProjectState.HASTESTERRORS));

        assertThat(jobStatus.getFailingModulePath(), is(repoDir.getCanonicalPath()));
        assertThat(gatherTestInformation.getNbTotalTests(), is(26));
        assertThat(gatherTestInformation.getNbFailingTests(), is(0));
        assertThat(gatherTestInformation.getNbErroringTests(), is(5));
        assertThat(gatherTestInformation.getNbSkippingTests(), is(0));

        Set<String> failureNames = jobStatus.getFailureNames();

        assertThat(failureNames.contains("java.lang.NullPointerException"), is(true));
        assertThat(failureNames.size(), is(3));

        assertThat(jobStatus.getFailureLocations().size(), is(4));
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
        System.out.println("Dirpath : "+tmpDirPath);


        File repoDir = new File(tmpDir, "repo");

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL, "");

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        GatherTestInformation gatherTestInformation = new GatherTestInformation(inspector, new BuildShouldFail(), false);


        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(new TestProject(inspector)).setNextStep(gatherTestInformation);
        cloneStep.execute();

        assertThat(gatherTestInformation.shouldStop, is(true));
        assertThat(gatherTestInformation.getState(), is(ProjectState.NOTFAILING));
        assertThat(jobStatus.getState(), is(ProjectState.NOTFAILING));

        assertThat(jobStatus.getFailingModulePath(), nullValue());
        assertThat(gatherTestInformation.getNbTotalTests(), is(1));
        assertThat(gatherTestInformation.getNbFailingTests(), is(0));
        assertThat(gatherTestInformation.getNbErroringTests(), is(0));
        assertThat(gatherTestInformation.getNbSkippingTests(), is(0));

        Set<String> failureNames = jobStatus.getFailureNames();
        assertThat(failureNames.size(), is(0));
        assertThat(jobStatus.getFailureLocations().size(), is(0));
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
        System.out.println("Dirpath : "+tmpDirPath);

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, ScannedBuildStatus.ONLY_FAIL, "");

        ProjectInspector inspector = mock(ProjectInspector.class);
        when(inspector.getWorkspace()).thenReturn(tmpDir.getAbsolutePath());
        when(inspector.getRepoLocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getBuildToBeInspected()).thenReturn(toBeInspected);
        when(inspector.getBuild()).thenReturn(build);
        when(inspector.getM2LocalPath()).thenReturn(tmpDir.getAbsolutePath()+"/.m2");
        when(inspector.getGitHelper()).thenReturn(new GitHelper());

        JobStatus jobStatus = new JobStatus(tmpDir.getAbsolutePath()+"/repo");
        when(inspector.getJobStatus()).thenReturn(jobStatus);

        CloneRepository cloneStep = new CloneRepository(inspector);
        GatherTestInformation gatherTestInformation = new GatherTestInformation(inspector, new BuildShouldPass(), false);


        cloneStep.setNextStep(new CheckoutBuild(inspector)).setNextStep(new TestProject(inspector)).setNextStep(gatherTestInformation);
        cloneStep.execute();

        assertThat(gatherTestInformation.shouldStop, is(false));
        assertThat(gatherTestInformation.getState(), is(ProjectState.NOTFAILING));
        assertThat(jobStatus.getState(), is(ProjectState.NOTFAILING));

        assertThat(jobStatus.getFailingModulePath(), nullValue());
        assertThat(gatherTestInformation.getNbTotalTests(), is(1));
        assertThat(gatherTestInformation.getNbFailingTests(), is(0));
        assertThat(gatherTestInformation.getNbErroringTests(), is(0));
        assertThat(gatherTestInformation.getNbSkippingTests(), is(0));

        Set<String> failureNames = jobStatus.getFailureNames();
        assertThat(failureNames.size(), is(0));
        assertThat(jobStatus.getFailureLocations().size(), is(0));
    }
}

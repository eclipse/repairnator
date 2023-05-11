package fr.inria.spirals.repairnator.process.step.repair;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.files.FileHelper;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.utils.Utils;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

/**
 * Created by urli on 11/07/2017.
 */
public class TestNPERepair {

    private File tmpDir;

    @Before
    public void setup() {
        Utils.setLoggersLevel(Level.ERROR);
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setJTravisEndpoint("https://api.travis-ci.com");
    }

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
        FileHelper.deleteFile(tmpDir);
    }

    @Test
    @Ignore
    //TODO: Add a test with a failing build from GitHub Actions
    public void testNPERepair() throws IOException {
        long buildId = 220951790; // repairnator/failingProject simple-npe rerun on 23/01/13
        RepairnatorConfig.getInstance().setOutputPath(Files.createTempDirectory("test_nperepair_output").toFile().getAbsolutePath());

        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_nperepair").toFile();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        RepairnatorConfig.getInstance().setRepairTools(Collections.singleton(NPERepair.TOOL_NAME));
        ProjectInspector inspector = new ProjectInspector(toBeInspected, tmpDir.getAbsolutePath(), null, null);

        CloneRepository cloneStep = new CloneRepository(inspector);
        NPERepair npeRepair = new NPERepair();
        npeRepair.setProjectInspector(inspector);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true))
                .addNextStep(new BuildProject(inspector))
                .addNextStep(new TestProject(inspector))
                .addNextStep(new ComputeClasspath(inspector, false))
                .addNextStep(new ComputeSourceDir(inspector, false, false))
                .addNextStep(new ComputeTestDir(inspector, true))
                .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
                .addNextStep(npeRepair);

        cloneStep.execute();

        assertFalse(npeRepair.isShouldStop());

        List<StepStatus> stepStatusList = inspector.getJobStatus().getStepStatuses();
        assertEquals(stepStatusList.size(), 9);
        StepStatus npeStatus = stepStatusList.get(8);
        assertThat(npeStatus.getStep(), is(npeRepair));

        for (StepStatus stepStatus : stepStatusList) {
            assertTrue(stepStatus.isSuccess());
        }

        String finalStatus = AbstractDataSerializer.getPrettyPrintState(inspector);
        assertThat(finalStatus, is("PATCHED"));

        List<RepairPatch> allPatches = inspector.getJobStatus().getAllPatches();
        assertThat(allPatches.size(), is(6));
        assertThat(inspector.getJobStatus().getToolDiagnostic().get(npeRepair.getRepairToolName()), notNullValue());

        for (RepairPatch repairPatch : allPatches) {
            assertTrue(new File(repairPatch.getFilePath()).exists());
        }
    }

    @Test
    @Ignore
    //TODO: Add a test with a failing build from GitHub Actions
    public void testNPERepairClassScope() throws IOException {
        long buildId = 220951924; // repairnator/failingProject npefix-scope
        RepairnatorConfig.getInstance().setNPEScope("class");
        RepairnatorConfig.getInstance().setOutputPath(Files.createTempDirectory("test_nperepair_output").toFile().getAbsolutePath());

        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_nperepair").toFile();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        RepairnatorConfig.getInstance().setRepairTools(Collections.singleton(NPERepair.TOOL_NAME));
        ProjectInspector inspector = new ProjectInspector(toBeInspected, tmpDir.getAbsolutePath(), null, null);

        CloneRepository cloneStep = new CloneRepository(inspector);
        NPERepair npeRepair = new NPERepair();
        npeRepair.setProjectInspector(inspector);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true))
                .addNextStep(new BuildProject(inspector))
                .addNextStep(new TestProject(inspector))
                .addNextStep(new ComputeClasspath(inspector, false))
                .addNextStep(new ComputeSourceDir(inspector, false, false))
                .addNextStep(new ComputeTestDir(inspector, true))
                .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
                .addNextStep(npeRepair);

        cloneStep.execute();

        assertThat(npeRepair.isShouldStop(), is(false));

        List<StepStatus> stepStatusList = inspector.getJobStatus().getStepStatuses();
        assertEquals(stepStatusList.size(), 9);
        StepStatus npeStatus = stepStatusList.get(8);
        assertThat(npeStatus.getStep(), is(npeRepair));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        String finalStatus = AbstractDataSerializer.getPrettyPrintState(inspector);
        assertThat(finalStatus, is("PATCHED"));

        List<RepairPatch> allPatches = inspector.getJobStatus().getAllPatches();
        assertThat(allPatches.size(), is(23));
        assertThat(inspector.getJobStatus().getToolDiagnostic().get(npeRepair.getRepairToolName()), notNullValue());

        for (RepairPatch repairPatch : allPatches) {
            assertTrue(new File(repairPatch.getFilePath()).exists());
        }
    }

    @Test
    @Ignore
    //TODO: Add a test with a failing build from GitHub Actions
    public void testNPERepairPackageScope() throws IOException {
        long buildId = 220951924; // repairnator/failingProject npefix-scope
        RepairnatorConfig.getInstance().setNPEScope("package");
        RepairnatorConfig.getInstance().setOutputPath(Files.createTempDirectory("test_nperepair_output").toFile().getAbsolutePath());

        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_nperepair").toFile();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        RepairnatorConfig.getInstance().setRepairTools(Collections.singleton(NPERepair.TOOL_NAME));
        ProjectInspector inspector = new ProjectInspector(toBeInspected, tmpDir.getAbsolutePath(), null, null);

        CloneRepository cloneStep = new CloneRepository(inspector);
        NPERepair npeRepair = new NPERepair();
        npeRepair.setProjectInspector(inspector);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true))
                .addNextStep(new BuildProject(inspector))
                .addNextStep(new TestProject(inspector))
                .addNextStep(new ComputeClasspath(inspector, false))
                .addNextStep(new ComputeSourceDir(inspector, false, false))
                .addNextStep(new ComputeTestDir(inspector, true))
                .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
                .addNextStep(npeRepair);

        cloneStep.execute();

        assertThat(npeRepair.isShouldStop(), is(false));

        List<StepStatus> stepStatusList = inspector.getJobStatus().getStepStatuses();
        assertEquals(stepStatusList.size(), 9);
        StepStatus npeStatus = stepStatusList.get(8);
        assertThat(npeStatus.getStep(), is(npeRepair));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        String finalStatus = AbstractDataSerializer.getPrettyPrintState(inspector);
        assertThat(finalStatus, is("PATCHED"));

        List<RepairPatch> allPatches = inspector.getJobStatus().getAllPatches();
        assertThat(allPatches.size(), is(23));
        assertThat(inspector.getJobStatus().getToolDiagnostic().get(npeRepair.getRepairToolName()), notNullValue());

        for (RepairPatch repairPatch : allPatches) {
            assertTrue(new File(repairPatch.getFilePath()).exists());
        }
    }

    @Test
    @Ignore
    //TODO: Add a test with a failing build from GitHub Actions
    public void testNPERepairStackScope() throws IOException {
        long buildId = 220951924; // repairnator/failingProject npefix-scope rerun on 23/01/13
        RepairnatorConfig.getInstance().setNPEScope("stack");
        RepairnatorConfig.getInstance().setOutputPath(Files.createTempDirectory("test_nperepair_output").toFile().getAbsolutePath());

        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_nperepair").toFile();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        RepairnatorConfig.getInstance().setRepairTools(Collections.singleton(NPERepair.TOOL_NAME));
        ProjectInspector inspector = new ProjectInspector(toBeInspected, tmpDir.getAbsolutePath(), null, null);

        CloneRepository cloneStep = new CloneRepository(inspector);
        NPERepair npeRepair = new NPERepair();
        npeRepair.setProjectInspector(inspector);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true))
                .addNextStep(new BuildProject(inspector))
                .addNextStep(new TestProject(inspector))
                .addNextStep(new ComputeClasspath(inspector, false))
                .addNextStep(new ComputeSourceDir(inspector, false, false))
                .addNextStep(new ComputeTestDir(inspector, true))
                .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
                .addNextStep(npeRepair);

        cloneStep.execute();

        assertThat(npeRepair.isShouldStop(), is(false));

        List<StepStatus> stepStatusList = inspector.getJobStatus().getStepStatuses();
        assertEquals(stepStatusList.size(), 9);
        StepStatus npeStatus = stepStatusList.get(8);
        assertThat(npeStatus.getStep(), is(npeRepair));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        String finalStatus = AbstractDataSerializer.getPrettyPrintState(inspector);
        assertThat(finalStatus, is("PATCHED"));

        List<RepairPatch> allPatches = inspector.getJobStatus().getAllPatches();
        assertThat(allPatches.size(), is(23));
        assertThat(inspector.getJobStatus().getToolDiagnostic().get(npeRepair.getRepairToolName()), notNullValue());

        for (RepairPatch repairPatch : allPatches) {
            assertTrue(new File(repairPatch.getFilePath()).exists());
        }
    }

    @Test
    @Ignore
    //TODO: Add a test with a failing build from GitHub Actions
    public void testNPERepairProjectScope() throws IOException {
        long buildId = 220951924; // repairnator/failingProject npefix-scope
        RepairnatorConfig.getInstance().setNPEScope("project");
        RepairnatorConfig.getInstance().setOutputPath(Files.createTempDirectory("test_nperepair_output").toFile().getAbsolutePath());

        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_nperepair").toFile();

        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

        RepairnatorConfig.getInstance().setRepairTools(Collections.singleton(NPERepair.TOOL_NAME));
        ProjectInspector inspector = new ProjectInspector(toBeInspected, tmpDir.getAbsolutePath(), null, null);

        CloneRepository cloneStep = new CloneRepository(inspector);
        NPERepair npeRepair = new NPERepair();
        npeRepair.setProjectInspector(inspector);

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true))
                .addNextStep(new BuildProject(inspector))
                .addNextStep(new TestProject(inspector))
                .addNextStep(new ComputeClasspath(inspector, false))
                .addNextStep(new ComputeSourceDir(inspector, false, false))
                .addNextStep(new ComputeTestDir(inspector, true))
                .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
                .addNextStep(npeRepair);

        cloneStep.execute();

        assertThat(npeRepair.isShouldStop(), is(false));

        List<StepStatus> stepStatusList = inspector.getJobStatus().getStepStatuses();
        assertEquals(stepStatusList.size(), 9);
        StepStatus npeStatus = stepStatusList.get(8);
        assertThat(npeStatus.getStep(), is(npeRepair));

        for (StepStatus stepStatus : stepStatusList) {
            assertTrue(stepStatus.isSuccess());
        }

        String finalStatus = AbstractDataSerializer.getPrettyPrintState(inspector);
        assertThat(finalStatus, is("PATCHED"));

        List<RepairPatch> allPatches = inspector.getJobStatus().getAllPatches();
        assertEquals(allPatches.size(), 23);
        assertThat(inspector.getJobStatus().getToolDiagnostic().get(npeRepair.getRepairToolName()), notNullValue());

        for (RepairPatch repairPatch : allPatches) {
            assertTrue(new File(repairPatch.getFilePath()).exists());
        }
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

package fr.inria.spirals.repairnator.process.inspectors;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.step.repair.NPERepair;
import fr.inria.spirals.repairnator.process.step.repair.nopol.NopolSingleTestRepair;
import fr.inria.spirals.repairnator.utils.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.files.FileHelper;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.utils4tests.Utils4Tests;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestJobStatus {

    private File tmpDir;

    @Before
    public void setup() {
        RepairnatorConfig config = RepairnatorConfig.getInstance();
        config.setZ3solverPath(Utils4Tests.getZ3SolverPath());
        Utils.setLoggersLevel(Level.ERROR);
    }

    @After
    public void tearDown() throws IOException {
        RepairnatorConfig.deleteInstance();
        FileHelper.deleteFile(tmpDir);
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

    @Test
    public void testGatheringPatches() throws IOException {
        long buildId = 382484026; // surli/failingProject build
        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_gathering").toFile();
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");
        ProjectInspector inspector = new ProjectInspector(toBeInspected, tmpDir.getAbsolutePath(), null, null);

        CloneRepository cloneStep = new CloneRepository(inspector);
        NopolSingleTestRepair nopolRepair = new NopolSingleTestRepair();
        nopolRepair.setProjectInspector(inspector);
        NPERepair npeRepair = new NPERepair();
        npeRepair.setProjectInspector(inspector);

        RepairnatorConfig.getInstance().setRepairTools(new HashSet<>(Arrays.asList(nopolRepair.getRepairToolName(), npeRepair.getRepairToolName())));

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true))
            .addNextStep(new TestProject(inspector))
            .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
            .addNextStep(new ComputeClasspath(inspector, true))
            .addNextStep(new ComputeSourceDir(inspector, true, false))
            .addNextStep(nopolRepair)
            .addNextStep(npeRepair);
        cloneStep.execute();

        assertThat(nopolRepair.isShouldStop(), is(false));
        assertThat(npeRepair.isShouldStop(), is(false));

        List<StepStatus> stepStatusList = inspector.getJobStatus().getStepStatuses();
        assertThat(stepStatusList.size(), is(8));
        StepStatus nopolStatus = stepStatusList.get(6);
        assertThat(nopolStatus.getStep(), is(nopolRepair));
        StepStatus npeStatus = stepStatusList.get(7);
        assertThat(npeStatus.getStep(), is(npeRepair));

        for (StepStatus stepStatus : stepStatusList) {
            assertThat(stepStatus.isSuccess(), is(true));
        }

        String finalStatus = AbstractDataSerializer.getPrettyPrintState(inspector);
        assertThat(finalStatus, is("PATCHED"));

        List<RepairPatch> allPatches = inspector.getJobStatus().getAllPatches();
        assertThat(allPatches.size(), is(16)); // 12 (nopol) + 4 (npe)
        assertThat(inspector.getJobStatus().getToolDiagnostic().get(nopolRepair.getRepairToolName()), notNullValue());
    }

    @Test
    public void testRankingPatches() throws IOException {
        long buildId = 382484026; // surli/failingProject build
        Build build = this.checkBuildAndReturn(buildId, false);

        tmpDir = Files.createTempDirectory("test_ranking").toFile();
        BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");
        ProjectInspector inspector = new ProjectInspector(toBeInspected, tmpDir.getAbsolutePath(), null, null);

        CloneRepository cloneStep = new CloneRepository(inspector);
        NopolSingleTestRepair nopolRepair = new NopolSingleTestRepair();
        nopolRepair.setProjectInspector(inspector);
        NPERepair npeRepair = new NPERepair();
        npeRepair.setProjectInspector(inspector);

        RepairnatorConfig.getInstance().setRepairTools(new HashSet<>(Arrays.asList(nopolRepair.getRepairToolName(), npeRepair.getRepairToolName())));

        cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true))
            .addNextStep(new TestProject(inspector))
            .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
            .addNextStep(new ComputeClasspath(inspector, true))
            .addNextStep(new ComputeSourceDir(inspector, true, false))
            .addNextStep(nopolRepair)
            .addNextStep(npeRepair);
        cloneStep.execute();

        List<RepairPatch> rankedPatches = inspector.getJobStatus().getRankedPatches();
        assertThat(rankedPatches.size(), is(16)); // 12 (nopol) + 4 (npe)

        assertThat(rankedPatches.get(0).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatches.get(0).getFilePath(), endsWith("modelo/Solver.java"));
        assertThat(rankedPatches.get(0).getOverfittingScore(), is(-3190.603359887649));
        assertThat(rankedPatches.get(1).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatches.get(1).getFilePath(), endsWith("Stub/ProxyPlatoStub.java"));
        assertThat(rankedPatches.get(1).getOverfittingScore(), is(-2775.6773763492697));
        assertThat(rankedPatches.get(2).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatches.get(2).getFilePath(), endsWith("modelo/ApiDB.java"));
        assertThat(rankedPatches.get(2).getOverfittingScore(), is(-1544.2008323597074));
        assertThat(rankedPatches.get(3).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatches.get(3).getFilePath(), endsWith("modelo/InternalDB.java"));
        assertThat(rankedPatches.get(3).getOverfittingScore(), is(-1011.203142967915));
        assertThat(rankedPatches.get(4).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatches.get(4).getFilePath(), endsWith("modelo/ControllerInternalDB.java"));
        assertThat(rankedPatches.get(4).getOverfittingScore(), is(-732.199911962387));
        assertThat(rankedPatches.get(5).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatches.get(5).getFilePath(), endsWith("modelo/InternalDB.java"));
        assertThat(rankedPatches.get(5).getOverfittingScore(), is(-589.2194952242268));
        assertThat(rankedPatches.get(6).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatches.get(6).getFilePath(), endsWith("modelo/ControllerInternalDB.java"));
        assertThat(rankedPatches.get(6).getOverfittingScore(), is(-525.0518968819456));
        assertThat(rankedPatches.get(7).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatches.get(7).getFilePath(), endsWith("Validadores/ValidadorPlato.java"));
        assertThat(rankedPatches.get(7).getOverfittingScore(), is(-41.19421176656324));
        assertThat(rankedPatches.get(8).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatches.get(8).getFilePath(), endsWith("modelo/InternalDB.java"));
        assertThat(rankedPatches.get(8).getOverfittingScore(), is(-41.19421176656324));
        assertThat(rankedPatches.get(9).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatches.get(9).getFilePath(), endsWith("modelo/ControllerInternalDB.java"));
        assertThat(rankedPatches.get(9).getOverfittingScore(), is(-41.19421176656324));
        assertThat(rankedPatches.get(10).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatches.get(10).getFilePath(), endsWith("modelo/ControllerInternalDB.java"));
        assertThat(rankedPatches.get(10).getOverfittingScore(), is(230.96289712519297));
        assertThat(rankedPatches.get(11).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatches.get(11).getFilePath(), endsWith("modelo/ControllerInternalDB.java"));
        assertThat(rankedPatches.get(11).getOverfittingScore(), is(3465.24494158228));

        // in following cases, OverfittingScore can not be computed because of invalid filePaths
        assertThat(rankedPatches.get(12).getToolname(), is("NPEFix"));
        assertThat(rankedPatches.get(12).getOverfittingScore(), is(Double.POSITIVE_INFINITY));
        assertThat(rankedPatches.get(13).getToolname(), is("NPEFix"));
        assertThat(rankedPatches.get(13).getOverfittingScore(), is(Double.POSITIVE_INFINITY));
        assertThat(rankedPatches.get(14).getToolname(), is("NPEFix"));
        assertThat(rankedPatches.get(14).getOverfittingScore(), is(Double.POSITIVE_INFINITY));
        assertThat(rankedPatches.get(15).getToolname(), is("NPEFix"));
        assertThat(rankedPatches.get(15).getOverfittingScore(), is(Double.POSITIVE_INFINITY));
    }
}

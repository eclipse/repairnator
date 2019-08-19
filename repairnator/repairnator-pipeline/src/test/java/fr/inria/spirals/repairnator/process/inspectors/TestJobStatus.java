package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.inspectors.properties.features.Features;
import fr.inria.spirals.repairnator.process.step.repair.NPERepair;
import fr.inria.spirals.repairnator.process.step.repair.nopol.NopolSingleTestRepair;
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

        // test ranking by P4J overfitting-scores
        List<RepairPatch> rankedPatchesP4J = inspector.getJobStatus().getRankedPatches();
        assertThat(rankedPatchesP4J.size(), is(16)); // 12 (nopol) + 4 (npe)

        assertThat(rankedPatchesP4J.get(0).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesP4J.get(0).getFilePath(), endsWith("modelo/Solver.java"));
        assertThat(rankedPatchesP4J.get(0).getOverfittingScore(Features.P4J), is(-3190.603359887649));
        assertThat(rankedPatchesP4J.get(1).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesP4J.get(1).getFilePath(), endsWith("Stub/ProxyPlatoStub.java"));
        assertThat(rankedPatchesP4J.get(1).getOverfittingScore(Features.P4J), is(-2775.6773763492697));
        assertThat(rankedPatchesP4J.get(2).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesP4J.get(2).getFilePath(), endsWith("modelo/ApiDB.java"));
        assertThat(rankedPatchesP4J.get(2).getOverfittingScore(Features.P4J), is(-1544.2008323597074));
        assertThat(rankedPatchesP4J.get(3).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesP4J.get(3).getFilePath(), endsWith("modelo/InternalDB.java"));
        assertThat(rankedPatchesP4J.get(3).getOverfittingScore(Features.P4J), is(-1011.203142967915));
        assertThat(rankedPatchesP4J.get(4).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesP4J.get(4).getFilePath(), endsWith("modelo/ControllerInternalDB.java"));
        assertThat(rankedPatchesP4J.get(4).getOverfittingScore(Features.P4J), is(-732.199911962387));
        assertThat(rankedPatchesP4J.get(5).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesP4J.get(5).getFilePath(), endsWith("modelo/InternalDB.java"));
        assertThat(rankedPatchesP4J.get(5).getOverfittingScore(Features.P4J), is(-589.2194952242268));
        assertThat(rankedPatchesP4J.get(6).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesP4J.get(6).getFilePath(), endsWith("modelo/ControllerInternalDB.java"));
        assertThat(rankedPatchesP4J.get(6).getOverfittingScore(Features.P4J), is(-525.0518968819456));
        assertThat(rankedPatchesP4J.get(7).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesP4J.get(7).getFilePath(), endsWith("Validadores/ValidadorPlato.java"));
        assertThat(rankedPatchesP4J.get(7).getOverfittingScore(Features.P4J), is(-41.19421176656324));
        assertThat(rankedPatchesP4J.get(8).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesP4J.get(8).getFilePath(), endsWith("modelo/InternalDB.java"));
        assertThat(rankedPatchesP4J.get(8).getOverfittingScore(Features.P4J), is(-41.19421176656324));
        assertThat(rankedPatchesP4J.get(9).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesP4J.get(9).getFilePath(), endsWith("modelo/ControllerInternalDB.java"));
        assertThat(rankedPatchesP4J.get(9).getOverfittingScore(Features.P4J), is(-41.19421176656324));
        assertThat(rankedPatchesP4J.get(10).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesP4J.get(10).getFilePath(), endsWith("modelo/ControllerInternalDB.java"));
        assertThat(rankedPatchesP4J.get(10).getOverfittingScore(Features.P4J), is(230.96289712519297));
        assertThat(rankedPatchesP4J.get(11).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesP4J.get(11).getFilePath(), endsWith("modelo/ControllerInternalDB.java"));
        assertThat(rankedPatchesP4J.get(11).getOverfittingScore(Features.P4J), is(3465.24494158228));
        // in following cases, OverfittingScore can not be computed because of invalid filePaths
        assertThat(rankedPatchesP4J.get(12).getToolname(), is("NPEFix"));
        assertThat(rankedPatchesP4J.get(12).getOverfittingScore(Features.P4J), is(Double.POSITIVE_INFINITY));
        assertThat(rankedPatchesP4J.get(13).getToolname(), is("NPEFix"));
        assertThat(rankedPatchesP4J.get(13).getOverfittingScore(Features.P4J), is(Double.POSITIVE_INFINITY));
        assertThat(rankedPatchesP4J.get(14).getToolname(), is("NPEFix"));
        assertThat(rankedPatchesP4J.get(14).getOverfittingScore(Features.P4J), is(Double.POSITIVE_INFINITY));
        assertThat(rankedPatchesP4J.get(15).getToolname(), is("NPEFix"));
        assertThat(rankedPatchesP4J.get(15).getOverfittingScore(Features.P4J), is(Double.POSITIVE_INFINITY));

        // test ranking by S4R overfitting-scores
        List<RepairPatch> rankedPatchesS4R = inspector.getJobStatus().getRankedPatches(Features.S4R);
        assertThat(rankedPatchesS4R.size(), is(16)); // 12 (nopol) + 4 (npe)

        assertThat(rankedPatchesS4R.get(0).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesS4R.get(0).getFilePath(), endsWith("Validadores/ValidadorPlato.java"));
        assertThat(rankedPatchesS4R.get(0).getOverfittingScore(Features.S4R), is(-0.020023754025157423));
        assertThat(rankedPatchesS4R.get(1).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesS4R.get(1).getFilePath(), endsWith("modelo/InternalDB.java"));
        assertThat(rankedPatchesS4R.get(1).getOverfittingScore(Features.S4R), is(-0.020023754025157423));
        assertThat(rankedPatchesS4R.get(2).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesS4R.get(2).getFilePath(), endsWith("modelo/InternalDB.java"));
        assertThat(rankedPatchesS4R.get(2).getOverfittingScore(Features.S4R), is(-0.020023754025157423));
        assertThat(rankedPatchesS4R.get(3).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesS4R.get(3).getFilePath(), endsWith("modelo/ControllerInternalDB.java"));
        assertThat(rankedPatchesS4R.get(3).getOverfittingScore(Features.S4R), is(-0.020023754025157423));
        assertThat(rankedPatchesS4R.get(4).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesS4R.get(4).getFilePath(), endsWith("modelo/ControllerInternalDB.java"));
        assertThat(rankedPatchesS4R.get(4).getOverfittingScore(Features.S4R), is(-0.020023754025157423));
        assertThat(rankedPatchesS4R.get(5).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesS4R.get(5).getFilePath(), endsWith("modelo/ControllerInternalDB.java"));
        assertThat(rankedPatchesS4R.get(5).getOverfittingScore(Features.S4R), is(-0.020023754025157423));
        assertThat(rankedPatchesS4R.get(6).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesS4R.get(6).getFilePath(), endsWith("modelo/InternalDB.java"));
        assertThat(rankedPatchesS4R.get(6).getOverfittingScore(Features.S4R), is(5.067401178818903E-4));
        assertThat(rankedPatchesS4R.get(7).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesS4R.get(7).getFilePath(), endsWith("modelo/Solver.java"));
        assertThat(rankedPatchesS4R.get(7).getOverfittingScore(Features.S4R), is(5.067401178818903E-4));
        assertThat(rankedPatchesS4R.get(8).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesS4R.get(8).getFilePath(), endsWith("modelo/ControllerInternalDB.java"));
        assertThat(rankedPatchesS4R.get(8).getOverfittingScore(Features.S4R), is(5.067401178818903E-4));
        assertThat(rankedPatchesS4R.get(9).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesS4R.get(9).getFilePath(), endsWith("modelo/ApiDB.java"));
        assertThat(rankedPatchesS4R.get(9).getOverfittingScore(Features.S4R), is(5.067401178818903E-4));
        assertThat(rankedPatchesS4R.get(10).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesS4R.get(10).getFilePath(), endsWith("Stub/ProxyPlatoStub.java"));
        assertThat(rankedPatchesS4R.get(10).getOverfittingScore(Features.S4R), is(0.010518617130460602));
        assertThat(rankedPatchesS4R.get(11).getToolname(), is("NopolSingleTest"));
        assertThat(rankedPatchesS4R.get(11).getFilePath(), endsWith("modelo/ControllerInternalDB.java"));
        assertThat(rankedPatchesS4R.get(11).getOverfittingScore(Features.S4R), is(0.010518617130460602));
        // in following cases, OverfittingScore can not be computed because of invalid filePaths
        assertThat(rankedPatchesS4R.get(12).getToolname(), is("NPEFix"));
        assertThat(rankedPatchesS4R.get(12).getOverfittingScore(Features.S4R), is(Double.POSITIVE_INFINITY));
        assertThat(rankedPatchesS4R.get(13).getToolname(), is("NPEFix"));
        assertThat(rankedPatchesS4R.get(13).getOverfittingScore(Features.S4R), is(Double.POSITIVE_INFINITY));
        assertThat(rankedPatchesS4R.get(14).getToolname(), is("NPEFix"));
        assertThat(rankedPatchesS4R.get(14).getOverfittingScore(Features.S4R), is(Double.POSITIVE_INFINITY));
        assertThat(rankedPatchesS4R.get(15).getToolname(), is("NPEFix"));
        assertThat(rankedPatchesS4R.get(15).getOverfittingScore(Features.S4R), is(Double.POSITIVE_INFINITY));
    }
}

package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.coming.codefeatures.RepairnatorFeatures.ODSLabel;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.files.FileHelper;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.repair.NPERepair;
import fr.inria.spirals.repairnator.process.step.repair.nopol.NopolSingleTestRepair;
import fr.inria.spirals.repairnator.process.utils4tests.Utils4Tests;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

// test the collection of all patches over different repair tools
public class TestGlobalPatchAnalysis {

	private File tmpDir;

	@Before
	public void setup() {
		RepairnatorConfig config = RepairnatorConfig.getInstance();
		config.setZ3solverPath(Utils4Tests.getZ3SolverPath());
		config.setJTravisEndpoint("https://api.travis-ci.com");
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
	@Ignore
	public void testGatheringPatches() throws IOException {
		long buildId = 220925392; // repairnator/failingProject build https://api.travis-ci.com/v3/build/220925392
		Build build = this.checkBuildAndReturn(buildId, false);

		tmpDir = Files.createTempDirectory("test_gathering").toFile();
		BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");
		ProjectInspector inspector = new ProjectInspector(toBeInspected, tmpDir.getAbsolutePath(), null, null);

		CloneRepository cloneStep = new CloneRepository(inspector);
		NopolSingleTestRepair nopolRepair = new NopolSingleTestRepair();
		nopolRepair.setProjectInspector(inspector);
		NPERepair npeRepair = new NPERepair();
		npeRepair.setProjectInspector(inspector);

		RepairnatorConfig.getInstance().setRepairTools(
				new HashSet<>(Arrays.asList(nopolRepair.getRepairToolName(), npeRepair.getRepairToolName())));

		cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true)).addNextStep(new TestProject(inspector))
				.addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
				.addNextStep(new ComputeClasspath(inspector, true))
				.addNextStep(new ComputeSourceDir(inspector, true, false)).addNextStep(nopolRepair)
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
		assertThat(allPatches.size(), is(87)); // 12 (nopol) + 4 (npe)
		assertThat(inspector.getJobStatus().getToolDiagnostic().get(nopolRepair.getRepairToolName()), notNullValue());
	}

	@Test
	public void testODSPatchClassification() throws IOException {
		RepairnatorConfig.getInstance().setPatchClassification(true);
		RepairnatorConfig.getInstance().setPatchClassificationMode(RepairnatorConfig.PATCH_CLASSIFICATION_MODE.ODS);

		long buildId = 225936611; // https://travis-ci.com/github/repairnator/TestingProject/builds/225936611
		Build build = this.checkBuildAndReturn(buildId, false);

		tmpDir = Files.createTempDirectory("patch_classification").toFile();
		new File(tmpDir.getAbsolutePath() + "/ODSPatches").mkdirs();
		RepairnatorConfig.getInstance().setODSPath(tmpDir.getAbsolutePath() + "/ODSPatches");
		
		BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");
		ProjectInspector inspector = new ProjectInspector(toBeInspected, tmpDir.getAbsolutePath(), null, null);

		CloneRepository cloneStep = new CloneRepository(inspector);
		NopolSingleTestRepair nopolRepair = new NopolSingleTestRepair();
		nopolRepair.setProjectInspector(inspector);

		RepairnatorConfig.getInstance().setRepairTools(new HashSet<>(Arrays.asList(nopolRepair.getRepairToolName())));

		cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true)).addNextStep(new TestProject(inspector))
				.addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
				.addNextStep(new ComputeClasspath(inspector, true))
				.addNextStep(new ComputeSourceDir(inspector, true, false))
				.addNextStep(nopolRepair);
		cloneStep.execute();

		List<RepairPatch> patches = inspector.getJobStatus().getAllPatches();

		// There are 1 patch (10 with Nopol working are generated for this failing build
		assertThat(patches.size(), is(1));

		int overfittingCount = 0;

		for(RepairPatch patch : patches) {
			if (ODSLabel.OVERFITTING == patch.getODSLabel()){
				overfittingCount++;
			}
		}
		
		assertThat(overfittingCount, is(1));
	}
	
}

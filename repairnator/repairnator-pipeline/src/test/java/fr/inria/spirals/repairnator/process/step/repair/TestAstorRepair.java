package fr.inria.spirals.repairnator.process.step.repair;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.files.FileHelper;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir;
import fr.inria.spirals.repairnator.process.step.repair.astor.AstorJKaliRepair;
import fr.inria.spirals.repairnator.process.step.repair.astor.AstorJMutRepair;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestAstorRepair {

    private File tmpDir;

    @Before
	public void setup() {
		Utils.setLoggersLevel(Level.ERROR);
	}

    @After
    public void tearDown() throws IOException {
        FileHelper.deleteFile(tmpDir);
    }

	@Test
	public void testAstorJkali() throws IOException {
		long buildId = 376820338; // surli/failingProject astor-jkali-failure

        Build build = this.checkBuildAndReturn(buildId, false);

		AstorJKaliRepair astorJKaliRepair = new AstorJKaliRepair();

		RepairnatorConfig.getInstance().setRepairTools(Collections.singleton(astorJKaliRepair.getRepairToolName()));
		tmpDir = Files.createTempDirectory("test_astorjkali").toFile();

		BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

		ProjectInspector inspector = new ProjectInspector(toBeInspected, tmpDir.getAbsolutePath(), null, null);

		CloneRepository cloneStep = new CloneRepository(inspector);

		astorJKaliRepair.setProjectInspector(inspector);

		cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true))
				.addNextStep(new TestProject(inspector))
				.addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
				.addNextStep(new ComputeClasspath(inspector, true))
				.addNextStep(new ComputeSourceDir(inspector, true, false))
				.addNextStep(new ComputeTestDir(inspector, true))
				.addNextStep(astorJKaliRepair);
		cloneStep.execute();

		assertThat(astorJKaliRepair.isShouldStop(), is(false));

		List<StepStatus> stepStatusList = inspector.getJobStatus().getStepStatuses();
		assertThat(stepStatusList.size(), is(8));
		StepStatus assertFixerStatus = stepStatusList.get(7);
		assertThat(assertFixerStatus.getStep(), is(astorJKaliRepair));

		for (StepStatus stepStatus : stepStatusList) {
			assertThat("Failing step :" + stepStatus, stepStatus.isSuccess(), is(true));
		}

		String finalStatus = AbstractDataSerializer.getPrettyPrintState(inspector);
		assertThat(finalStatus, is("PATCHED"));
		List<RepairPatch> allPatches = inspector.getJobStatus().getAllPatches();
		assertThat(allPatches.isEmpty(), is(false));
		assertThat(inspector.getJobStatus().getToolDiagnostic().get(astorJKaliRepair.getRepairToolName()), notNullValue());
	}

	@Test
	public void testAstorJMut() throws IOException {
		long buildId = 376847154; // surli/failingProject astor-jkali-failure

        Build build = this.checkBuildAndReturn(buildId, false);

		AstorJMutRepair astorJMutRepair = new AstorJMutRepair();

		RepairnatorConfig.getInstance().setRepairTools(Collections.singleton(astorJMutRepair.getRepairToolName()));
		tmpDir = Files.createTempDirectory("test_astorjkali").toFile();

		BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

		ProjectInspector inspector = new ProjectInspector(toBeInspected, tmpDir.getAbsolutePath(), null, null);

		CloneRepository cloneStep = new CloneRepository(inspector);

		astorJMutRepair.setProjectInspector(inspector);

		cloneStep.addNextStep(new CheckoutBuggyBuild(inspector, true))
				.addNextStep(new TestProject(inspector))
				.addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
				.addNextStep(new ComputeClasspath(inspector, true))
				.addNextStep(new ComputeSourceDir(inspector, true, false))
				.addNextStep(new ComputeTestDir(inspector, true))
				.addNextStep(astorJMutRepair);
		cloneStep.execute();

		assertThat(astorJMutRepair.isShouldStop(), is(false));

		List<StepStatus> stepStatusList = inspector.getJobStatus().getStepStatuses();
		assertThat(stepStatusList.size(), is(8));
		StepStatus assertFixerStatus = stepStatusList.get(7);
		assertThat(assertFixerStatus.getStep(), is(astorJMutRepair));

		for (StepStatus stepStatus : stepStatusList) {
			assertThat("Failing step :" + stepStatus, stepStatus.isSuccess(), is(true));
		}

		String finalStatus = AbstractDataSerializer.getPrettyPrintState(inspector);
		assertThat(finalStatus, is("PATCHED"));
		List<RepairPatch> allPatches = inspector.getJobStatus().getAllPatches();
		assertThat(allPatches.isEmpty(), is(false));
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

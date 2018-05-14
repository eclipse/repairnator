package fr.inria.spirals.repairnator.process.step.repair;

import ch.qos.logback.classic.Level;
import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.step.CloneRepository;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestAstorJKaliRepair {
	@Before
	public void setup() {
		Utils.setLoggersLevel(Level.ERROR);
	}

	@Test
	public void testAstorJkali() throws IOException {
		long buildId = 376820338; // surli/failingProject astor-jkali-failure

		Optional<Build> optionalBuild = RepairnatorConfig.getInstance().getJTravis().build().fromId(buildId);
		assertTrue(optionalBuild.isPresent());
		Build build = optionalBuild.get();
		assertThat(build, notNullValue());
		assertThat(buildId, is(build.getId()));

		RepairnatorConfig.getInstance().setRepairTools(Collections.singleton(AstorJKaliRepair.TOOL_NAME));
		Path tmpDirPath = Files.createTempDirectory("test_astorjkali");
		File tmpDir = tmpDirPath.toFile();
		tmpDir.deleteOnExit();

		BuildToBeInspected toBeInspected = new BuildToBeInspected(build, null, ScannedBuildStatus.ONLY_FAIL, "");

		ProjectInspector inspector = new ProjectInspector(toBeInspected, tmpDir.getAbsolutePath(), null, null);

		CloneRepository cloneStep = new CloneRepository(inspector);
		AstorJKaliRepair astorJKaliRepair = new AstorJKaliRepair();
		astorJKaliRepair.setProjectInspector(inspector);

		cloneStep.setNextStep(new CheckoutBuggyBuild(inspector, true))
				.setNextStep(new TestProject(inspector))
				.setNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
				.setNextStep(new ComputeClasspath(inspector, true))
				.setNextStep(new ComputeSourceDir(inspector, true, false))
				.setNextStep(new ComputeTestDir(inspector, true))
				.setNextStep(astorJKaliRepair);
		cloneStep.execute();

		assertThat(astorJKaliRepair.isShouldStop(), is(false));

		// FIXME: distinguish kind of patches
		assertThat(inspector.getJobStatus().getAstorPatches().isEmpty(), is(false));

		List<StepStatus> stepStatusList = inspector.getJobStatus().getStepStatuses();
		assertThat(stepStatusList.size(), is(8));
		StepStatus assertFixerStatus = stepStatusList.get(7);
		assertThat(assertFixerStatus.getStep(), is(astorJKaliRepair));

		for (StepStatus stepStatus : stepStatusList) {
			assertThat("Failing step :" + stepStatus, stepStatus.isSuccess(), is(true));
		}

		String finalStatus = AbstractDataSerializer.getPrettyPrintState(inspector);
		assertThat(finalStatus, is("PATCHED"));
	}
}

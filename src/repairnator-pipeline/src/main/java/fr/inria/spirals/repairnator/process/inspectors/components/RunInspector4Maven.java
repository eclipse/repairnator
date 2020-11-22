package fr.inria.spirals.repairnator.process.inspectors.components;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.process.step.WritePropertyFile;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeModules;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir;

import java.util.List;
import java.util.stream.Collectors;

public class RunInspector4Maven extends IRunInspector {
	private List<AbstractStep> repairSteps;

	public RunInspector4Maven(List<AbstractStep> repairSteps) {
		this.repairSteps = repairSteps;
	}

	@Override
	public void run(ProjectInspector inspector) {
		this.repairSteps.stream().peek(repairStep -> repairStep.setProjectInspector(inspector)).collect(Collectors.toList());

		AbstractStep buildProjectStep = new BuildProject(inspector);

		buildProjectStep.addNextStep(new TestProject(inspector))
				.addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
				.addNextStep(new ComputeClasspath(inspector, false))
				.addNextStep(new ComputeSourceDir(inspector, false, false))
				.addNextStep(new ComputeTestDir(inspector, false))
				.addNextSteps(repairSteps);

		AbstractStep finalStep = new ComputeModules(inspector, false);
		finalStep.addNextStep(new WritePropertyFile(inspector));

		inspector.setFinalStep(finalStep);

		inspector.printPipeline();

		buildProjectStep.execute();
	}
}

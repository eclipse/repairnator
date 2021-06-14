package fr.inria.spirals.repairnator.process.inspectors.components;

import fr.inria.spirals.repairnator.notifier.ErrorNotifier;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.*;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.paths.ComputePlugins;
import fr.inria.spirals.repairnator.process.step.push.PushProcessEnd;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import fr.inria.spirals.repairnator.process.step.repair.styler.StylerRepair;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;

public class RunInspector4StylerRepair extends IRunInspector {

	public RunInspector4StylerRepair() {

	}

	@Override
	public void run(ProjectInspector inspector) {
		// TODO: Finish
		AbstractStep cloneRepo = new CloneRepository(inspector);

		AbstractRepairStep stylerRepairStep = new StylerRepair();
		stylerRepairStep.setProjectInspector(inspector);
		stylerRepairStep.addNextStep(new PushProcessEnd(inspector));

		BranchingStep branchingStep = new BranchingStep(inspector, cloneRepo);
		branchingStep.createBranch(StepStatus.StatusKind.FAILURE, stylerRepairStep);
		branchingStep.createBranch(StepStatus.StatusKind.SUCCESS, new PushProcessEnd(inspector));

		cloneRepo.addNextStep(new CheckoutBuggyBuild(inspector, true))
				.addNextStep(new BuildProject(inspector, false))
				.addNextStep(new ComputePlugins(inspector, false))
				.addBranchingStep(branchingStep);

		try {
			cloneRepo.execute();
		} catch (Exception e) {
			inspector.getJobStatus().addStepError("Unknown", e.getMessage());
			inspector.getLogger().error("Exception catch while executing steps: ", e);
			inspector.getJobStatus().setFatalError(e);

			ErrorNotifier errorNotifier = ErrorNotifier.getInstance();
			if (errorNotifier != null) {
				errorNotifier.observe(inspector);
			}

			for (AbstractDataSerializer serializer : inspector.getSerializers()) {
				serializer.serialize();
			}
		}
	}

}

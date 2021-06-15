package fr.inria.spirals.repairnator.process.inspectors.components;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherCheckstyleInformation;
import fr.inria.spirals.repairnator.process.step.paths.ComputePlugins;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import fr.inria.spirals.repairnator.process.step.repair.styler.StylerRepair;

public class RunInspector4StylerRepair extends IRunInspector {

	@Override
	public void run(ProjectInspector inspector) {
		AbstractStep computePlugins = new ComputePlugins(inspector, true);

		AbstractRepairStep stylerRepair = new StylerRepair();

		computePlugins.
				addNextStep(new GatherCheckstyleInformation(inspector, true))
				.addNextStep(stylerRepair);

		stylerRepair.setProjectInspector(inspector);
	}
}

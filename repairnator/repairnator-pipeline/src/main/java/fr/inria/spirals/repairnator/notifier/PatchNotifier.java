package fr.inria.spirals.repairnator.notifier;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.RepairPatch;

import java.util.List;

/** gets notified with new patches */
public interface PatchNotifier {
	void notify(ProjectInspector inspector, String toolname, List<RepairPatch> patches);
}

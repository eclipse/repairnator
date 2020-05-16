package fr.inria.spirals.repairnator.process.inspectors.components;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;

public abstract class IRunInspector {
	public boolean skipPreSteps;
	public abstract void run(ProjectInspector inspector);

	public IRunInspector setSkipPreSteps(boolean skipPreSteps) {
		this.skipPreSteps = skipPreSteps;
		return this;
	};
}
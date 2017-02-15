package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildStatus;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;

/**
 * Created by fermadeiral.
 */
public class InspectPreviousBuild extends AbstractStep {

	public InspectPreviousBuild(ProjectInspector inspector) {
		super(inspector);
	}

	@Override
	protected void businessExecute() {
		if (this.inspector instanceof ProjectInspector4Bears) {
			
			this.getLogger().debug("Inspecting previous build...");
			
			((ProjectInspector4Bears)this.inspector).setPreviousBuildFlag(true);
			
			Build previousBuild = BuildHelper.getLastBuildOfSameBranchOfStatusBeforeBuild(this.inspector.getBuild(), null);
			
			if (previousBuild != null) {
				((ProjectInspector4Bears)this.inspector).setPreviousBuild(previousBuild);
				
				this.getLogger().debug("Build: " + this.inspector.getBuild().getId());
				this.getLogger().debug("Previous build: " + previousBuild.getId());
				
				if (previousBuild.getBuildStatus() == BuildStatus.FAILED) {
					this.shouldStop = false;
				} else {
					if (previousBuild.getBuildStatus() == BuildStatus.PASSED) {
						// Other case...
					} else {
						this.shouldStop = true;
					}
				}
			} else {
				this.shouldStop = true;
			}
		}
	}

}
package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.jtravis.entities.BuildStatus;
import fr.inria.spirals.jtravis.helpers.BuildHelper;
import fr.inria.spirals.repairnator.process.ProjectState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;

/**
 * Created by fermadeiral.
 */
public class TestPreviousBuild extends AbstractStep {

	public TestPreviousBuild(ProjectInspector inspector) {
        super(inspector);
    }

	@Override
	protected void businessExecute() {
		this.getLogger().debug("Testing previous build...");
		
		Build previousBuild = BuildHelper.getLastBuildOfSameBranchOfStatusBeforeBuild(this.inspector.getBuild(), null);
		
		if (previousBuild.getBuildStatus() == BuildStatus.FAILED) {
			ProjectInspector4Bears inspector = new ProjectInspector4Bears(previousBuild, this.inspector.getWorkspace(), null, null, false, this.inspector.getMode(), true);
            inspector.setAutoclean(this.inspector.isAutoclean());
            inspector.run();
            
            if (inspector.getState() == ProjectState.HASTESTFAILURE) {
            	// So, 1) the current passing build can be reproduced and 2) its previous build is a failing build with failing tests and it can also be reproduced
            	this.state = ProjectState.FIXERBUILD;
            	this.shouldStop = false; // The next step will be to push both builds.
            } else {
            	this.shouldStop = true;
            }
		} else {
			this.shouldStop = true;
		}
	}
}
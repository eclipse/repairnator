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
					AbstractStep testPreviousFailingBuild = new TestPreviousFailingBuild(this.inspector, previousBuild);
					AbstractStep buildRepo = new BuildProject(this.inspector);
			        AbstractStep testProject = new TestProject(this.inspector);
			        AbstractStep gatherTestInformation = new GatherTestInformation(this.inspector);
					
			        this.setNextStep(testPreviousFailingBuild).setNextStep(buildRepo).setNextStep(testProject).setNextStep(gatherTestInformation);
			        
					if (this.inspector.getPushMode()) {
						PushIncriminatedBuild pushIncriminatedBuild = new PushIncriminatedBuild(this.inspector);
						pushIncriminatedBuild.setRemoteRepoUrl(PushIncriminatedBuild.REMOTE_REPO_BEAR);
			            this.inspector.setPushBuild(pushIncriminatedBuild);
			            gatherTestInformation.setNextStep(pushIncriminatedBuild);
					}
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
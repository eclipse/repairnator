package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.GithubInputBuild;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.process.inspectors.components.*;

import java.util.List;

public class InspectorFactory {

	public static ProjectInspector getTravisInspector(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractNotifier> notifiers) {
		return new ProjectInspector(buildToBeInspected,workspace,notifiers).setIRunInspector(new RunInspector4DefaultTravis());
	}

	public static ProjectInspector getSequencerRepairInspector(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractNotifier> notifiers){
		return new ProjectInspector(buildToBeInspected,workspace,notifiers).setIRunInspector(new RunInspector4SequencerRepair());
	}

	public static ProjectInspector getFaultLocalizationInspector(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractNotifier> notifiers){
		return new ProjectInspector(buildToBeInspected,workspace,notifiers).setIRunInspector(new RunInspector4FaultLocalization());
	}

	public static GitRepositoryProjectInspector getGithubInspector(GithubInputBuild build, boolean isGitRepositoryFirstCommit,
    		String workspace, List<AbstractNotifier> notifiers) {
		GitRepositoryProjectInspector inspector = new GitRepositoryProjectInspector(build, isGitRepositoryFirstCommit, workspace, notifiers);

		switch (RepairnatorConfig.getInstance().getLauncherMode()){
			case SEQUENCER_REPAIR:
				inspector.setIRunInspector(new RunInspector4SequencerRepair());
				break;
			case FEEDBACK:
				if (RepairnatorConfig.getInstance().getCommandFunctionality()){
					inspector.setIRunInspector(new RunInspector4FeedbackGit(true));
				}
				else {
					inspector.setIRunInspector(new RunInspector4FeedbackGit());
					}
				break;
			default:
				inspector.setIRunInspector(new RunInspector4DefaultGit());
				break;
		}
		return inspector;
	}


	public static ProjectInspector4Bears getBearsInspector(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractNotifier> notifiers) {
		ProjectInspector4Bears inspector = new ProjectInspector4Bears(buildToBeInspected,workspace,notifiers);
		inspector.setIRunInspector(new RunInspector4Bears());
		return inspector;
	}

	public static ProjectInspector4Checkstyle getCheckStyleInspector(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractNotifier> notifiers) {
		ProjectInspector4Checkstyle inspector = new ProjectInspector4Checkstyle(buildToBeInspected,workspace,notifiers);
		inspector.setIRunInspector(new RunInspector4CheckStyle());
		return inspector;
	}

	public static ProjectInspector getMavenInspector(String workspace, List<AbstractNotifier> notifiers) {
		ProjectInspector inspector = new ProjectInspector(workspace, notifiers);
		inspector.setIRunInspector(new RunInspector4Maven());
		return inspector;
	}
}

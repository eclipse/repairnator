package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.inspectors.components.*;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;

import java.util.List;

public class InspectorFactory {

	public static ProjectInspector getTravisInspector(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractNotifier> notifiers) {
		return new ProjectInspector(buildToBeInspected,workspace,notifiers).setIRunInspector(new RunInspector4DefaultTravis());
	}

	public static ProjectInspector getSequencerRepairInspector(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractNotifier> notifiers){
		return new ProjectInspector(buildToBeInspected,workspace,notifiers).setIRunInspector(new RunInspector4SequencerRepair());
	}

	public static GitRepositoryProjectInspector getGithubInspector(String gitRepoUrl, String gitRepoBranch, String gitRepoIdCommit, boolean isGitRepositoryFirstCommit,
    		String workspace, List<AbstractNotifier> notifiers) {
		GitRepositoryProjectInspector inspector = new GitRepositoryProjectInspector(gitRepoUrl,gitRepoBranch,gitRepoIdCommit,isGitRepositoryFirstCommit,workspace,notifiers);
		inspector.setIRunInspector(new RunInspector4DefaultGit());
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
}
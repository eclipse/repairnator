package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.process.inspectors.components.RunInspector4DefaultTravis;
import fr.inria.spirals.repairnator.process.inspectors.components.RunInspector4DefaultGit;
import fr.inria.spirals.repairnator.process.inspectors.components.RunInspector4Bears;
import fr.inria.spirals.repairnator.process.inspectors.components.RunInspector4CheckStyle;

import java.util.List;

public class InspectorFactory {
	

	public static ProjectInspector getDefaultTravisInspector(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractNotifier> notifiers) {
		return new ProjectInspector(buildToBeInspected,workspace,notifiers).setIRunInspector(new RunInspector4DefaultTravis());
	}


	public static ProjectInspector getStaticAnalysisTravisInspector(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractNotifier> notifiers) {
		return new ProjectInspector(buildToBeInspected,workspace,notifiers).setIRunInspector(new RunInspector4DefaultTravis(true));
	}

	public static ProjectInspector getDefaultGithubInspector(String gitRepoUrl, String gitRepoBranch, String gitRepoIdCommit, boolean isGitRepositoryFirstCommit,
    		String workspace, List<AbstractNotifier> notifiers) {
		return new GitRepositoryProjectInspector(gitRepoUrl,gitRepoBranch,gitRepoIdCommit,isGitRepositoryFirstCommit,workspace,notifiers).setIRunInspector(new RunInspector4DefaultGit());
	}
	

	public static ProjectInspector getStaticAnalysisGithubInspector(String gitRepoUrl, String gitRepoBranch, String gitRepoIdCommit, boolean isGitRepositoryFirstCommit,
    		String workspace, List<AbstractNotifier> notifiers) {
		return new GitRepositoryProjectInspector(gitRepoUrl,gitRepoBranch,gitRepoIdCommit,isGitRepositoryFirstCommit,workspace,notifiers).setIRunInspector(new RunInspector4DefaultGit(true));
	}


	public static ProjectInspector getDefaultBearsInspector(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractNotifier> notifiers) {
		return new ProjectInspector4Bears(buildToBeInspected,workspace,notifiers).setIRunInspector(new RunInspector4Bears());
	}

	public static ProjectInspector getDefaultCheckStyleInspector(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractNotifier> notifiers) {
		return new ProjectInspector4Checkstyle(buildToBeInspected,workspace,notifiers).setIRunInspector(new RunInspector4CheckStyle());
	}
}
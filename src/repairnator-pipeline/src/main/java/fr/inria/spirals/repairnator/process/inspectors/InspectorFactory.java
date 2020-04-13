package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.process.inspectors.components.RunInspector4DefaultTravis;
import fr.inria.spirals.repairnator.process.inspectors.components.RunInspector4DefaultGit;

import java.util.List;

public class InspectorFactory {
	

	public static ProjectInspector getDefaultTravisInspector(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractDataSerializer> serializers, List<AbstractNotifier> notifiers) {
		return new ProjectInspector(buildToBeInspected,workspace,serializers,notifiers).setIRunInspector(new RunInspector4DefaultTravis());
	}


	public static ProjectInspector getDefaultGithubInspector(String gitRepoUrl, String gitRepoBranch, String gitRepoIdCommit, boolean isGitRepositoryFirstCommit,
    		String workspace, List<AbstractDataSerializer> serializers, List<AbstractNotifier> notifiers) {
		return new GitRepositoryProjectInspector(gitRepoUrl,gitRepoBranch,gitRepoIdCommit,isGitRepositoryFirstCommit,workspace,serializers,notifiers).setIRunInspector(new RunInspector4DefaultGit());
	}

}
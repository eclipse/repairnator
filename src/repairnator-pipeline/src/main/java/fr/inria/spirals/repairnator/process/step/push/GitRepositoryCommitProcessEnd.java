package fr.inria.spirals.repairnator.process.step.push;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.properties.builds.Builds;
import fr.inria.spirals.repairnator.process.inspectors.properties.commits.Commits;

public class GitRepositoryCommitProcessEnd extends CommitProcessEnd {

   public GitRepositoryCommitProcessEnd(ProjectInspector inspector) {
        super(inspector);
    }

    @Override
    public String createCommitMsg() {
        String commitMsg = "";

        Commits commits = this.getInspector().getJobStatus().getProperties().getCommits();
        Builds builds = this.getInspector().getJobStatus().getProperties().getBuilds();
        switch (this.commitType) {
            case COMMIT_BUGGY_BUILD:
            	commitMsg = "Bug commit from " + getInspector().getGitSlug() + "\n";
            	break;

            case COMMIT_HUMAN_PATCH:
                commitMsg = "Human patch from " + this.getInspector().getRepoSlug() + "\n";

                commitMsg += "This commit is based on the source code from the following commit: " + commits.getFixerBuild().getUrl() + "\n";
                commitMsg += "The mentioned commit triggered the following Travis build: " + builds.getFixerBuild().getUrl() + ".";
                break;
                
            case COMMIT_REPAIR_INFO:
                commitMsg = "Automatic repair information (optional automatic patches)";
                break;

            case COMMIT_PROCESS_END:
                commitMsg = "End of the Repairnator process";
                break;

            case COMMIT_CHANGED_TESTS:
                commitMsg = "Changes in the tests\n";

                commitMsg += "This commit is based on the source code from the following commit: " + commits.getFixerBuild().getUrl() + "\n";
                commitMsg += "The mentioned commit triggered the following Travis build: " + builds.getFixerBuild().getUrl() + ".";
                break;
        }

        return commitMsg;
    }
}

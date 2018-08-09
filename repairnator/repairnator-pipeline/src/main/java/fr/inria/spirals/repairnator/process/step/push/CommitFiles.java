package fr.inria.spirals.repairnator.process.step.push;

import fr.inria.spirals.repairnator.process.files.FileHelper;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.properties.builds.Builds;
import fr.inria.spirals.repairnator.process.inspectors.properties.commits.Commits;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.states.LauncherMode;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;

public class CommitFiles extends AbstractStep {

    private CommitType commitType;
    private RevCommit commit;

    public CommitFiles(ProjectInspector inspector) {
        super(inspector, false);
    }

    public CommitFiles(ProjectInspector inspector, String stepName) {
        super(inspector, false, stepName);
    }

    @Override
    protected StepStatus businessExecute() {
        if (this.getConfig().isPush()) {

            File sourceDir = new File(this.getInspector().getRepoLocalPath());
            File targetDir = new File(this.getInspector().getRepoToPushLocalPath());

            GitHelper gitHelper = this.getInspector().getGitHelper();

            String[] excludedFileNames = {".git", ".m2"};
            if (this.commitType == CommitType.COMMIT_BUGGY_BUILD) {
                FileHelper.copyDirectory(sourceDir, targetDir, excludedFileNames, true, this);
            } else {
                FileHelper.copyDirectory(sourceDir, targetDir, excludedFileNames, false, this);
            }

            FileHelper.removeNotificationFromTravisYML(targetDir, this);

            FileHelper.removeGhOauthFromCreatedFilesToPush(targetDir, this.getInspector().getJobStatus().getCreatedFilesToPush());

            try {
                Git git;

                if (this.commitType == CommitType.COMMIT_BUGGY_BUILD) {
                    git = Git.init().setDirectory(targetDir).call();
                } else {
                    git = Git.open(targetDir);
                }

                git.add().addFilepattern(".").call();

                gitHelper.gitAdd(this.getInspector().getJobStatus().getCreatedFilesToPush(), git);

                String commitMsg = this.createCommitMsg();

                this.commit = git.commit().setMessage(commitMsg).setAuthor(GitHelper.getCommitterIdent()).setCommitter(GitHelper.getCommitterIdent()).call();

                return StepStatus.buildSuccess(this);
            } catch (GitAPIException e) {
                this.addStepError("Error while initializing the new local git repository.", e);
            } catch (IOException e) {
                this.addStepError("Error while opening the local git repository, maybe it has not been initialized.", e);
            }
            return StepStatus.buildSkipped(this, "Error while initializing or opening the local git repository.");
        } else {
            this.getLogger().info("Repairnator is configured NOT to push. Step bypassed.");
            return StepStatus.buildSkipped(this);
        }
    }

    public String createCommitMsg() {
        String commitMsg = "";

        Commits commits = this.getInspector().getJobStatus().getProperties().getCommits();
        Builds builds = this.getInspector().getJobStatus().getProperties().getBuilds();
        switch (this.commitType) {
            case COMMIT_BUGGY_BUILD:
                commitMsg = "Bug commit from " + this.getInspector().getRepoSlug() + "\n";

                commitMsg += "This commit is based on the source code from the following commit: " + commits.getBuggyBuild().getUrl() + "\n";
                commitMsg += "The mentioned commit triggered the following Travis build: " + builds.getBuggyBuild().getUrl() + ".";
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
                if (this.getConfig().getLauncherMode() == LauncherMode.BEARS) {
                    commitMsg = "End of the bug and patch reproduction process";
                } else {
                    commitMsg = "End of the Repairnator process";
                }
                break;

            case COMMIT_CHANGED_TESTS:
                commitMsg = "Changes in the tests\n";

                commitMsg += "This commit is based on the source code from the following commit: " + commits.getFixerBuild().getUrl() + "\n";
                commitMsg += "The mentioned commit triggered the following Travis build: " + builds.getFixerBuild().getUrl() + ".";
                break;
        }

        return commitMsg;
    }

    public void setCommitType(CommitType commitType) {
        this.commitType = commitType;
    }

    public RevCommit getCommit() {
        return commit;
    }
}

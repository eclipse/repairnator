package fr.inria.spirals.repairnator.process.step.push;

import fr.inria.spirals.repairnator.process.files.FileHelper;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.Metrics;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
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

        Metrics metrics;
        switch (this.commitType) {
            case COMMIT_BUGGY_BUILD:
                commitMsg = "Bug commit from the following repository " + this.getInspector().getRepoSlug() + "\n";

                metrics = this.getInspector().getJobStatus().getMetrics();
                commitMsg += "This bug commit is a reflect of source code from: " + metrics.getBugCommitUrl() + ".";
                break;

            case COMMIT_HUMAN_PATCH:
                commitMsg = "Human patch from the following repository "+this.getInspector().getRepoSlug()+"\n";

                metrics = this.getInspector().getJobStatus().getMetrics();
                commitMsg += "This commit is a reflect of the following : "+metrics.getPatchCommitUrl()+".";
                break;

            case COMMIT_REPAIR_INFO:
                commitMsg = "Automatic repair information (optional automatic patches)";
                break;

            case COMMIT_PROCESS_END:
                commitMsg = "End of the repairnator process";
                break;

            case COMMIT_CHANGED_TESTS:
                commitMsg = "Changes in the tests";
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

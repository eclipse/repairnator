package fr.inria.spirals.repairnator.process.step.push;

import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.Metrics;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.states.PushState;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;

import java.io.File;

/**
 * Created by urli on 26/04/2017.
 */
public class InitRepoToPush extends AbstractStep {

    public InitRepoToPush(ProjectInspector inspector) {
        super(inspector, false);
    }

    @Override
    protected StepStatus businessExecute() {

        if (this.getConfig().isPush()) {
            this.getLogger().info("Repairnator configured to push. Start init repo to push.");

            File sourceDir = new File(this.getInspector().getRepoLocalPath());
            File targetDir = new File(this.getInspector().getRepoToPushLocalPath());

            try {
                GitHelper gitHelper = this.getInspector().getGitHelper();

                String[] excludedFileNames = { ".git", ".m2" };
                gitHelper.copyDirectory(sourceDir, targetDir, excludedFileNames, this);

                gitHelper.removeNotificationFromTravisYML(targetDir, this);

                Git git = Git.init().setDirectory(targetDir).call();
                git.add().addFilepattern(".").call();

                gitHelper.gitAdd(this.getInspector().getJobStatus().getCreatedFilesToPush(), git);


                PersonIdent personIdent = new PersonIdent("Luc Esape", "luc.esape@gmail.com");
                String message = "Bug commit from the following repository "+this.getInspector().getRepoSlug()+"\n";

                Metrics metrics = this.getInspector().getJobStatus().getMetrics();
                message += "This bug commit is a reflect of source code from: "+metrics.getBugCommitUrl()+".";

                git.commit().setMessage(message)
                        .setAuthor(personIdent).setCommitter(personIdent).call();

                this.setPushState(PushState.REPO_INITIALIZED);
                return StepStatus.buildSuccess(this);
            } catch (GitAPIException e) {
                this.addStepError("Error while initializing the new git repository.", e);
                this.setPushState(PushState.REPO_NOT_INITIALIZED);
            }
            return StepStatus.buildSkipped(this, "Error while initializing the new git repository.");
        } else {
            this.getLogger().info("Repairnator configured to NOT push. Step bypassed.");
            return StepStatus.buildSkipped(this);
        }
    }
}

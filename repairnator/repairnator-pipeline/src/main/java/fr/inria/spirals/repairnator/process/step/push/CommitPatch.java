package fr.inria.spirals.repairnator.process.step.push;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.inspectors.Metrics;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.states.PushState;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;

/**
 * Created by urli on 27/04/2017.
 */
public class CommitPatch extends AbstractStep {
    private boolean pushHumanPatch;

    public CommitPatch(ProjectInspector inspector, boolean pushHumanPatch) {
        super(inspector, false);
        this.pushHumanPatch = pushHumanPatch;
    }

    public CommitPatch(ProjectInspector inspector, String name, boolean pushHumanPatch) {
        super(inspector, false, name);
        this.pushHumanPatch = pushHumanPatch;
    }

    @Override
    protected StepStatus businessExecute() {
        if (RepairnatorConfig.getInstance().isPush()) {
            this.getLogger().info("Start the step push patch");

            File sourceDir = new File(this.getInspector().getRepoLocalPath());
            File targetDir = new File(this.getInspector().getRepoToPushLocalPath());

            // FIXME: all git stuff should be supported through GitHelper
            try {
                Git git = Git.open(targetDir);
                Ref oldHeadRef = git.getRepository().exactRef("HEAD");

                RevWalk revWalk = new RevWalk(git.getRepository());
                RevCommit headRev = revWalk.parseCommit(oldHeadRef.getObjectId());
                revWalk.dispose();

                GitHelper gitHelper = this.getInspector().getGitHelper();

                String[] excludedFileNames = { ".git", ".m2" };
                gitHelper.copyDirectory(sourceDir, targetDir, excludedFileNames, this);

                gitHelper.removeNotificationFromTravisYML(targetDir, this);

                git.add().addFilepattern(".").call();

                gitHelper.gitAdd(this.getInspector().getJobStatus().getCreatedFilesToPush(), git);



                String commitMsg;
                if (pushHumanPatch) {
                    commitMsg = "Human patch from the following repository "+this.getInspector().getRepoSlug()+"\n";

                    Metrics metrics = this.getInspector().getJobStatus().getMetrics();
                    commitMsg += "This commit is a reflect of the following : "+metrics.getPatchCommitUrl()+".";
                } else {
                    commitMsg = "Automatic repair information (optionally automatic patches).";
                }

                PersonIdent personIdent = new PersonIdent("Luc Esape", "luc.esape@gmail.com");
                RevCommit commit = git.commit().setMessage(commitMsg)
                        .setAuthor(personIdent).setCommitter(personIdent).call();

                this.getInspector().getGitHelper().computePatchStats(this.getInspector().getJobStatus().getMetrics(), git, headRev, commit);

                if (this.getInspector().getJobStatus().isHasBeenPushed()) {
                    CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(this.getConfig().getGithubToken(), "");
                    git.push().setRemote(PushIncriminatedBuild.REMOTE_NAME).setCredentialsProvider(credentialsProvider).call();
                }

                if (pushHumanPatch) {
                    this.setPushState(PushState.PATCH_COMMITTED);
                } else {
                    this.setPushState(PushState.REPAIR_INFO_COMMITTED);
                }
                return StepStatus.buildSuccess(this);
            } catch (IOException e) {
                this.addStepError("Error while opening the git repository, maybe it has not been initialized yet.", e);
            } catch (GitAPIException e) {
                this.addStepError("Error while pushing or committed info.", e);
            }
            return StepStatus.buildSkipped(this,"Error while pushing or committed info.");
        } else {
            this.getLogger().info("Repairnator is configured to not push anything. This step will be bypassed.");
            return StepStatus.buildSkipped(this);
        }
    }
}

package fr.inria.spirals.repairnator.process.step.push;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.states.PushState;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

import java.io.File;
import java.io.IOException;

/**
 * Created by urli on 27/04/2017.
 */
public class CommitPatch extends CommitFiles {

    private CommitType commitType;

    public CommitPatch(ProjectInspector inspector, CommitType commitType) {
        super(inspector);
        this.commitType = commitType;
    }

    public CommitPatch(ProjectInspector inspector, String name, CommitType commitType) {
        super(inspector, name);
        this.commitType = commitType;
    }

    @Override
    protected StepStatus businessExecute() {
        if (this.getConfig().isPush()) {
            if (this.commitType == CommitType.COMMIT_HUMAN_PATCH) {
                this.getLogger().info("Commit human patch...");
            } else {
                this.getLogger().info("Commit info from repair tools...");
            }

            super.setCommitType(this.commitType);

            try {
                Git git = Git.open(new File(this.getInspector().getRepoToPushLocalPath()));
                Ref oldHeadRef = git.getRepository().exactRef("HEAD");

                RevWalk revWalk = new RevWalk(git.getRepository());
                RevCommit headRev = revWalk.parseCommit(oldHeadRef.getObjectId());
                revWalk.dispose();

                StepStatus stepStatus = super.businessExecute();

                if (stepStatus.isSuccess()) {
                    RevCommit commit = super.getCommit();
                    this.getInspector().getGitHelper().computePatchStats(this.getInspector().getJobStatus(), git, headRev, commit);

                    if (this.commitType == CommitType.COMMIT_HUMAN_PATCH) {
                        this.setPushState(PushState.PATCH_COMMITTED);
                    } else {
                        this.setPushState(PushState.REPAIR_INFO_COMMITTED);
                    }
                } else {
                    if (this.commitType == CommitType.COMMIT_HUMAN_PATCH) {
                        this.setPushState(PushState.PATCH_NOT_COMMITTED);
                    } else {
                        this.setPushState(PushState.REPAIR_INFO_NOT_COMMITTED);
                    }
                }
                return stepStatus;
            } catch (IOException e) {
                this.addStepError("Error while opening the local git repository, maybe it has not been initialized.", e);
            }
            if (this.commitType == CommitType.COMMIT_HUMAN_PATCH) {
                this.setPushState(PushState.PATCH_NOT_COMMITTED);
            } else {
                this.setPushState(PushState.REPAIR_INFO_NOT_COMMITTED);
            }
            return StepStatus.buildSkipped(this,"Error while committing.");
        } else {
            this.getLogger().info("Repairnator is configured NOT to push. Step bypassed.");
            return StepStatus.buildSkipped(this);
        }
    }
}

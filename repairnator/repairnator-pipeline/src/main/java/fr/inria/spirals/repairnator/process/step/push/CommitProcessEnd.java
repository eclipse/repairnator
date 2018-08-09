package fr.inria.spirals.repairnator.process.step.push;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector4Bears;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.states.PushState;

public class CommitProcessEnd extends CommitFiles {

    public CommitProcessEnd(ProjectInspector inspector) {
        super(inspector);
    }

    @Override
    protected StepStatus businessExecute() {
        if (this.getConfig().isPush()) {
            if (this.getInspector().getJobStatus().getLastPushState() != PushState.NONE) {
                if (this.getInspector() instanceof ProjectInspector4Bears &&
                        !((ProjectInspector4Bears) this.getInspector()).isBug()) {
                    this.getLogger().error("The reproduction of the bug and/or the patch failed. Step bypassed.");
                    return StepStatus.buildSkipped(this, "The reproduction of the bug and/or the patch failed. Step bypassed.");
                }

                this.getLogger().info("Commit process end...");

                super.setCommitType(CommitType.COMMIT_PROCESS_END);

                StepStatus stepStatus = super.businessExecute();

                if (stepStatus.isSuccess()) {
                    this.setPushState(PushState.PROCESS_END_COMMITTED);
                } else {
                    this.setPushState(PushState.PROCESS_END_NOT_COMMITTED);
                }
                return stepStatus;
            } else {
                this.getLogger().info("No commit has been done, so the end of the process will NOT be committed. Step bypassed.");
                return StepStatus.buildSkipped(this);
            }
        } else {
            this.getLogger().info("Repairnator is configured NOT to push. Step bypassed.");
            return StepStatus.buildSkipped(this);
        }
    }
}

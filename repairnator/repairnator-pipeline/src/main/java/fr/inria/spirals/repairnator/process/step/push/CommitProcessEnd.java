package fr.inria.spirals.repairnator.process.step.push;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.states.PushState;

public class CommitProcessEnd extends CommitFiles {

    public CommitProcessEnd(ProjectInspector inspector) {
        super(inspector);
    }

    @Override
    protected StepStatus businessExecute() {
        if (this.getConfig().isPush()) {
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
            this.getLogger().info("Repairnator is configured NOT to push. Step bypassed.");
            return StepStatus.buildSkipped(this);
        }
    }
}

package fr.inria.spirals.repairnator.process.step.push;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.states.PushState;

public class CommitChangedTests extends CommitFiles {

    public CommitChangedTests(ProjectInspector inspector) {
        super(inspector);
    }

    @Override
    protected StepStatus businessExecute() {
        if (this.getConfig().isPush()) {
            this.getLogger().info("Commit changes in the tests...");

            super.setCommitType(CommitType.COMMIT_CHANGED_TESTS);

            StepStatus stepStatus = super.businessExecute();

            if (stepStatus.isSuccess()) {
                this.setPushState(PushState.CHANGED_TESTS_COMMITTED);
            } else {
                this.setPushState(PushState.CHANGED_TESTS_NOT_COMMITTED);
            }
            return stepStatus;
        } else {
            this.getLogger().info("Repairnator is configured NOT to push. Step bypassed.");
            return StepStatus.buildSkipped(this);
        }
    }

}

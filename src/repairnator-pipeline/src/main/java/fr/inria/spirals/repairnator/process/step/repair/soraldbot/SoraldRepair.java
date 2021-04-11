package fr.inria.spirals.repairnator.process.step.repair.soraldbot;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import fr.inria.spirals.repairnator.process.step.repair.soraldbot.models.SoraldTargetCommit;

import java.util.Arrays;
import java.util.List;

import static fr.inria.spirals.repairnator.process.step.repair.soraldbot.SoraldConstants.*;

public class SoraldRepair extends AbstractRepairStep {
    private SoraldTargetCommit commit;

    private void init(){
        commit = new SoraldTargetCommit(getInspector().getGitCommit(), getInspector().getRepoSlug());
    }

    @Override
    public String getRepairToolName() {
        return SORALD_TOOL_NAME;
    }

    @Override
    protected StepStatus businessExecute() {
        init();

        List<String> rules = Arrays.asList(RepairnatorConfig.getInstance().getSonarRules());
        try {
            SoraldCIAdapter.getInstance(getInspector().getWorkspace(), SPOON_SNIPER_MODE)
                    .repairAndCreateForks(commit, rules);
        } catch (Exception e) {
            return StepStatus.buildSkipped(this,"Error while repairing with Sorald");
        }

        return StepStatus.buildSuccess(this);
    }
}

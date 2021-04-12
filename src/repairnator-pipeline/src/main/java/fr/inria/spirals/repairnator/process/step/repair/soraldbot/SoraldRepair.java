package fr.inria.spirals.repairnator.process.step.repair.soraldbot;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import fr.inria.spirals.repairnator.process.step.repair.soraldbot.models.SoraldTargetCommit;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SoraldRepair extends AbstractRepairStep {
    private static final String REPO_PATH = "repo";
    private static final String RULE_LINK_TEMPLATE = "https://rules.sonarsource.com/java/RSPEC-";
    private SoraldTargetCommit commit;

    private void init() {
        commit = new SoraldTargetCommit(getInspector().getGitCommit(), getInspector().getRepoSlug());
    }

    @Override
    public String getRepairToolName() {
        return SoraldConstants.SORALD_TOOL_NAME;
    }

    @Override
    protected StepStatus businessExecute() {
        init();

        List<String> rules = Arrays.asList(RepairnatorConfig.getInstance().getSonarRules());
        try {
            for (String rule : rules) {
                Set<String> patchedFiles =
                        SoraldAdapter.getInstance(getInspector().getWorkspace(), SoraldConstants.SPOON_SNIPER_MODE)
                                .repairRepoAndReturnViolationIntroducingFiles(commit, rule, REPO_PATH);

            }
        } catch (Exception e) {
            return StepStatus.buildSkipped(this, "Error while repairing with Sorald");
        }

        return StepStatus.buildSuccess(this);
    }
}

package fr.inria.spirals.repairnator.process.step.repair.nopol;

import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.states.PipelineState;
import static fr.inria.spirals.repairnator.utils.Utils.checkToolsJar;

import java.util.Collections;

/**
 * This launch Nopol against all classes of failing test at once
 */
public class NopolAllTestsRepair extends AbstractNopolRepair {
    protected static final String TOOL_NAME = "NopolAllTests";

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().debug("Start to use nopol single repair to repair...");

        try {
            checkToolsJar();
        } catch (ClassNotFoundException e) {
            this.addStepError("tools.jar has not been provided. Nopol can't be launched.");
            return StepStatus.buildError(this, PipelineState.MISSING_DEPENDENCIES);
        }

        this.initPatchDir();
        this.initWithJobStatus();

        if (this.getClassPath() != null && this.getSources() != null) {
            this.runNopol(this.getInspector().getJobStatus().getFailureLocations(), Collections.EMPTY_LIST, true);

            return this.recordResults();
        } else {
            this.addStepError("No classpath or sources directory has been given. Nopol can't be launched.");
            return StepStatus.buildSkipped(this,"No classpath or source directory given.");
        }
    }

    @Override
    public String getRepairToolName() {
        return TOOL_NAME;
    }
}

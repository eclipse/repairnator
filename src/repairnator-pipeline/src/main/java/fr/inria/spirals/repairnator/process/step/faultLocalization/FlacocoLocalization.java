package fr.inria.spirals.repairnator.process.step.faultLocalization;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.JobStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.spoonlabs.flacoco.api.Flacoco;
import fr.spoonlabs.flacoco.api.result.FlacocoResult;
import fr.spoonlabs.flacoco.core.config.FlacocoConfig;

import java.io.File;
import java.net.URL;

public class FlacocoLocalization extends AbstractStep {

    public FlacocoLocalization(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
    }

    public FlacocoLocalization(ProjectInspector inspector, boolean blockingStep, String name) {
        super(inspector, blockingStep, name);
    }

    @Override
    protected StepStatus businessExecute() {
        FlacocoConfig config = setupFlacocoConfig();
        Flacoco flacoco = new Flacoco(config);
        FlacocoResult result = flacoco.run();

        this.getLogger().debug("Results from flacoco: " + result.getDefaultSuspiciousnessMap().toString());

        this.getInspector().getJobStatus().setFlacocoResult(result);
        return StepStatus.buildSuccess(this);
    }

    private FlacocoConfig setupFlacocoConfig() {
        FlacocoConfig flacocoConfig = new FlacocoConfig();
        JobStatus jobStatus = this.getInspector().getJobStatus();

        flacocoConfig.setProjectPath(jobStatus.getFailingModulePath());
        flacocoConfig.setClasspath(jobStatus.getRepairClassPath().stream()
                .map(URL::getPath).reduce((x, y) -> x + File.pathSeparator + y).orElse(""));
        flacocoConfig.setThreshold(RepairnatorConfig.getInstance().getFlacocoThreshold());

        return flacocoConfig;
    }

}
package fr.inria.spirals.repairnator.process.inspectors.components;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.pipeline.RepairToolsManager;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.process.step.WritePropertyFile;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeModules;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;

import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates ProjectInspector's behaviour for a local Maven project. It is meant to be used through `maven-repair`.
 *
 * The `workspace` of the pipeline should be the root of a Maven project.
 *
 * @author andre15silva
 */
public class RunInspector4Maven extends IRunInspector {
    /**
     * List of repair steps that will be executed.
     */
    private List<AbstractStep> repairSteps = new ArrayList<>();

    /**
     * Constructs pipeline and executes its steps.
     *
     * @param inspector
     */
    @Override
    public void run(ProjectInspector inspector) {
        AbstractStep buildProjectStep = new BuildProject(inspector);
        for (String repairToolName : RepairnatorConfig.getInstance().getRepairTools()) {
            AbstractRepairStep repairStep = RepairToolsManager.getStepFromName(repairToolName);
            if (repairStep != null) {
                repairStep.setProjectInspector(inspector);
                repairSteps.add(repairStep);
            } else {
                inspector.getLogger().error("Error while getting repair step class for following name: " + repairToolName);
            }
        }

        buildProjectStep.addNextStep(new TestProject(inspector))
                .addNextStep(new ComputeClasspath(inspector, false))
                .addNextStep(new ComputeSourceDir(inspector, false, false))
                .addNextStep(new ComputeTestDir(inspector, true))
                .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
                .addNextSteps(repairSteps);

        AbstractStep finalStep = new ComputeModules(inspector, false);
        finalStep.addNextStep(new WritePropertyFile(inspector));

        inspector.setFinalStep(finalStep);

        inspector.printPipeline();

        buildProjectStep.execute();
    }
}
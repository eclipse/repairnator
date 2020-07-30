package fr.inria.spirals.repairnator.process.inspectors.components;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.ErrorNotifier;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.*;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeModules;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir;
import fr.inria.spirals.repairnator.process.step.push.*;
import fr.inria.spirals.repairnator.process.step.repair.sequencer.SequencerRepair;
import fr.inria.spirals.repairnator.process.step.repair.sequencer.detection.AstorDetectionStrategy;
import fr.inria.spirals.repairnator.process.step.repair.sequencer.detection.LogParserDetectionStrategy;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;

public class RunInspector4SequencerRepair extends IRunInspector{

    public RunInspector4SequencerRepair() {}


	@Override
	public void run(ProjectInspector inspector) {

        AbstractStep cloneRepo = new CloneRepository(inspector);
        cloneRepo.addNextStep(new CheckoutBuggyBuild(inspector, true));
        cloneRepo.addNextStep(new BuildProject(inspector, false));

        AbstractStep initRepoStep = new InitRepoToPush(inspector);


        SequencerRepair testSequencerRepairStep = new SequencerRepair(new AstorDetectionStrategy());
        testSequencerRepairStep.setProjectInspector(inspector);

        AbstractStep testRepairStep = new TestProject(inspector);

        testRepairStep.addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
                .addNextStep(new ComputeClasspath(inspector, false))
                .addNextStep(new ComputeSourceDir(inspector, false, false))
                .addNextStep(new ComputeTestDir(inspector, false))
                .addNextStep(testSequencerRepairStep)
                .addNextStep(initRepoStep);

        AbstractStep buildRepairStep = new SequencerRepair(new LogParserDetectionStrategy());
        buildRepairStep.setProjectInspector(inspector);

        buildRepairStep.addNextStep(initRepoStep);

        BranchingStep branchingStep = new BranchingStep(inspector, testRepairStep);


        branchingStep.createBranch(StepStatus.StatusKind.FAILURE, buildRepairStep);
        branchingStep.createBranch(StepStatus.StatusKind.SUCCESS, testRepairStep);

        cloneRepo.addBranchingStep(branchingStep);

        initRepoStep.addNextStep(new CommitPatch(inspector, CommitType.COMMIT_REPAIR_INFO));

        AbstractStep finalStep = new ComputeModules(inspector, false);
        finalStep.
                addNextStep(new WritePropertyFile(inspector)).
                addNextStep(new CommitProcessEnd(inspector)).
                addNextStep(new PushProcessEnd(inspector));

        inspector.setFinalStep(finalStep);

        cloneRepo.setDataSerializer(inspector.getSerializers());
        cloneRepo.setNotifiers(inspector.getNotifiers());

        inspector.printPipeline();

        try {
            cloneRepo.execute();
        } catch (Exception e) {
            inspector.getJobStatus().addStepError("Unknown", e.getMessage());
            inspector.getLogger().error("Exception catch while executing steps: ", e);
            inspector.getJobStatus().setFatalError(e);

            ErrorNotifier errorNotifier = ErrorNotifier.getInstance();
            if (errorNotifier != null) {
                errorNotifier.observe(inspector);
            }

            for (AbstractDataSerializer serializer : inspector.getSerializers()) {
                serializer.serialize();
            }
        }
    }
}
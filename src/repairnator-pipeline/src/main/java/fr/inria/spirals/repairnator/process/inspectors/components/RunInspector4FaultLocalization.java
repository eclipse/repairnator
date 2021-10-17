package fr.inria.spirals.repairnator.process.inspectors.components;

import fr.inria.spirals.repairnator.notifier.ErrorNotifier;
import fr.inria.spirals.repairnator.process.inspectors.GitRepositoryProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.BuildProject;
import fr.inria.spirals.repairnator.process.step.CloneCheckoutBranchRepository;
import fr.inria.spirals.repairnator.process.step.TestProject;
import fr.inria.spirals.repairnator.process.step.faultLocalization.FlacocoLocalization;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir;
import fr.inria.spirals.repairnator.process.step.push.PushFaultLocalizationSuggestions;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;

public class RunInspector4FaultLocalization extends IRunInspector {

    @Override
    public void run(ProjectInspector inspector_in) {
        GitRepositoryProjectInspector inspector = (GitRepositoryProjectInspector) inspector_in;
        if (inspector.getGitRepositoryUrl() != null) {
            AbstractStep cloneRepo = new CloneCheckoutBranchRepository(inspector);

            cloneRepo
                    .addNextStep(new BuildProject(inspector))
                    .addNextStep(new TestProject(inspector))
                    .addNextStep(new ComputeClasspath(inspector, false))
                    .addNextStep(new ComputeSourceDir(inspector, false, false))
                    .addNextStep(new ComputeTestDir(inspector, true))
                    .addNextStep(new GatherTestInformation(inspector, true, new BuildShouldFail(), false))
                    .addNextStep(new FlacocoLocalization(inspector, true));

            PushFaultLocalizationSuggestions finalStep = new PushFaultLocalizationSuggestions(inspector, true);
            inspector.setFinalStep(finalStep);

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
        } else {
            inspector.getLogger().debug("Build " + inspector.getBuggyBuild().getId() + " is not a failing build.");
        }
    }
}

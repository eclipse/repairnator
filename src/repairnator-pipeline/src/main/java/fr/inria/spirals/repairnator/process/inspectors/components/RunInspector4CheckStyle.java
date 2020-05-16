package fr.inria.spirals.repairnator.process.inspectors.components;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.step.paths.ComputePlugins;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.push.*;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.process.step.*;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;

public class RunInspector4CheckStyle extends IRunInspector {

	@Override
	public void run (ProjectInspector inspector) {
		if (inspector.getBuildToBeInspected().getStatus() != ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES) {
            AbstractStep cloneRepo = new CloneRepository(inspector);
            cloneRepo
                .addNextStep(new CheckoutBuggyBuild(inspector, true))
                .addNextStep(new BuildProject(inspector))
                .addNextStep(new ComputePlugins(inspector, false))
                .addNextStep(new Checkstyle(inspector));

            inspector.setFinalStep(new ComputeSourceDir(inspector, false, true));


            inspector.getFinalStep().
                addNextStep(new WritePropertyFile(inspector)).
                addNextStep(new CommitProcessEnd(inspector)).
                addNextStep(new PushProcessEnd(inspector));

            cloneRepo.setDataSerializer(inspector.getSerializers());
            cloneRepo.setNotifiers(inspector.getNotifiers());

            inspector.printPipeline();

            try {
                cloneRepo.execute();
            } catch (Exception e) {
                inspector.getJobStatus().addStepError("Unknown", e.getMessage());
                inspector.getLogger().error("Exception catch while executing steps: ", e);
            }
        } else {
            inspector.getLogger().debug("Build " + inspector.getBuggyBuild().getId() + " is not a failing build.");
        }
	}
}

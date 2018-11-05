package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.step.paths.ComputePlugins;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.push.*;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.process.step.*;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by bloriot97.
 */
public class ProjectInspector4Checkstyle extends ProjectInspector {
    private final Logger logger = LoggerFactory.getLogger(ProjectInspector4Bears.class);

    public ProjectInspector4Checkstyle(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractDataSerializer> serializers, List<AbstractNotifier> notifiers) {
        super(buildToBeInspected, workspace, serializers, notifiers);
    }

    protected void initProperties() {
        this.getJobStatus().getProperties().setVersion("Checkstyle 0.1");
        super.initProperties();
    }

    public void run() {
        if (this.getBuildToBeInspected().getStatus() != ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES) {
            AbstractStep cloneRepo = new CloneRepository(this);
            cloneRepo
                .addNextStep(new CheckoutBuggyBuild(this, true))
                .addNextStep(new BuildProject(this))
                .addNextStep(new ComputePlugins(this, false))
                .addNextStep(new Checkstyle(this));

            super.setFinalStep(new ComputeSourceDir(this, false, true));


            super.getFinalStep().
                addNextStep(new WritePropertyFile(this)).
                addNextStep(new CommitProcessEnd(this)).
                addNextStep(new PushProcessEnd(this));

            cloneRepo.setDataSerializer(this.getSerializers());
            cloneRepo.setNotifiers(this.getNotifiers());

            this.printPipeline();

            try {
                cloneRepo.execute();
            } catch (Exception e) {
                this.getJobStatus().addStepError("Unknown", e.getMessage());
                this.logger.error("Exception catch while executing steps: ", e);
            }
        } else {
            this.logger.debug("Build " + this.getBuggyBuild().getId() + " is not a failing build.");
        }


    }

}

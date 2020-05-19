package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuildTestCode;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeModules;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir;
import fr.inria.spirals.repairnator.process.step.push.*;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.process.step.*;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutPatchedBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuildSourceCode;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldPass;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by fermadeiral.
 */
public class ProjectInspector4Bears extends ProjectInspector {
    private final Logger logger = LoggerFactory.getLogger(ProjectInspector4Bears.class);

    private boolean bug;
    private String bugType;

    public ProjectInspector4Bears(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractDataSerializer> serializers, List<AbstractNotifier> notifiers) {
        super(buildToBeInspected, workspace, serializers, notifiers);
        this.bug = false;
    }

    public ProjectInspector4Bears(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractNotifier> notifiers) {
        super(buildToBeInspected, workspace, notifiers);
        this.bug = false;
    }

    protected void initProperties() {
        this.getJobStatus().getProperties().setVersion("Bears 1.0");
        super.initProperties();
    }

    public String getRemoteBranchName() {
        return this.getRepoSlug().replace('/', '-') + '-' + this.getBuggyBuild().getId() + '-' + this.getPatchedBuild().getId();
    }

    public boolean isBug() {
        return bug;
    }

    public String getBugType() {
        return bugType;
    }

    public void setBug(boolean bug, String bugType) {
        this.bug = bug;
        this.bugType = bugType;
    }

}

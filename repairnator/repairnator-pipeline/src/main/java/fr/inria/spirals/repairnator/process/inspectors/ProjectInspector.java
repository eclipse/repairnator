package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.ErrorNotifier;
import fr.inria.spirals.repairnator.pipeline.RepairToolsManager;
import fr.inria.spirals.repairnator.process.step.pathes.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.pathes.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.pathes.ComputeTestDir;
import fr.inria.spirals.repairnator.process.step.push.InitRepoToPush;
import fr.inria.spirals.repairnator.process.step.push.PushIncriminatedBuild;
import fr.inria.spirals.repairnator.process.step.push.CommitPatch;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.process.git.GitHelper;
import fr.inria.spirals.repairnator.process.step.*;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutBuggyBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutPatchedBuild;
import fr.inria.spirals.repairnator.process.step.checkoutrepository.CheckoutType;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldFail;
import fr.inria.spirals.repairnator.process.step.gatherinfo.BuildShouldPass;
import fr.inria.spirals.repairnator.process.step.gatherinfo.GatherTestInformation;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by urli on 26/12/2016.
 */
public class ProjectInspector {
    private final Logger logger = LoggerFactory.getLogger(ProjectInspector.class);

    private GitHelper gitHelper;
    private BuildToBeInspected buildToBeInspected;
    private String repoLocalPath;
    private String repoToPushLocalPath;

    private String workspace;
    private String m2LocalPath;
    private List<AbstractDataSerializer> serializers;
    private JobStatus jobStatus;
    private List<AbstractNotifier> notifiers;

    private CheckoutType checkoutType;

    public ProjectInspector(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractDataSerializer> serializers, List<AbstractNotifier> notifiers) {
        this.buildToBeInspected = buildToBeInspected;

        this.workspace = workspace;
        this.repoLocalPath = workspace + File.separator + getRepoSlug() + File.separator + buildToBeInspected.getBuggyBuild().getId();
        this.repoToPushLocalPath = repoLocalPath+"_topush";
        this.m2LocalPath = new File(this.repoLocalPath + File.separator + ".m2").getAbsolutePath();
        this.serializers = serializers;
        this.gitHelper = new GitHelper();
        this.jobStatus = new JobStatus(repoLocalPath);
        this.notifiers = notifiers;
        this.checkoutType = CheckoutType.NO_CHECKOUT;
        this.initMetricsValue();
    }

    private void initMetricsValue() {
        try {
            Metrics metrics = this.jobStatus.getMetrics();
            metrics.setBuggyBuildId(this.getBuggyBuild().getId());
            metrics.setBuggyBuildURL(Utils.getTravisUrl(this.getBuggyBuild().getId(), this.getRepoSlug()));
            metrics.setBuggyBuildDate(this.getBuggyBuild().getFinishedAt());

            if (this.buildToBeInspected.getStatus() != ScannedBuildStatus.ONLY_FAIL) {
                metrics.setPatchedBuilId(this.getPatchedBuild().getId());
                metrics.setPatchedBuildURL(Utils.getTravisUrl(this.getPatchedBuild().getId(), this.getRepoSlug()));
                metrics.setPatchedBuildDate(this.getPatchedBuild().getFinishedAt());
            }
        } catch (Exception e) {
            this.logger.error("Error while initializing metrics value", e);
        }
    }

    public JobStatus getJobStatus() {
        return jobStatus;
    }

    public GitHelper getGitHelper() {
        return this.gitHelper;
    }

    public List<AbstractDataSerializer> getSerializers() {
        return serializers;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getM2LocalPath() {
        return m2LocalPath;
    }

    public BuildToBeInspected getBuildToBeInspected() {
        return this.buildToBeInspected;
    }

    public Build getPatchedBuild() {
        return this.buildToBeInspected.getPatchedBuild();
    }

    public Build getBuggyBuild() {
        return this.buildToBeInspected.getBuggyBuild();
    }

    public String getRepoSlug() {
        return this.buildToBeInspected.getBuggyBuild().getRepository().getSlug();
    }

    public String getRepoLocalPath() {
        return repoLocalPath;
    }

    public String getRepoToPushLocalPath() {
        return repoToPushLocalPath;
    }

    public String getRemoteBranchName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMdd-HHmmss");
        String formattedDate = dateFormat.format(this.getBuggyBuild().getFinishedAt());
        return this.getRepoSlug().replace('/', '-') + '-' + this.getBuggyBuild().getId() + '-' + formattedDate;
    }

    public void run() {
        if (this.buildToBeInspected.getStatus() != ScannedBuildStatus.PASSING_AND_PASSING_WITH_TEST_CHANGES) {
            AbstractStep cloneRepo = new CloneRepository(this);
            AbstractStep lastStep = cloneRepo
                    .setNextStep(new CheckoutBuggyBuild(this, true))
                    .setNextStep(new ComputeSourceDir(this, true, true)) // TODO: check, should it be really blocking?
                    .setNextStep(new ComputeTestDir(this, true))                    // IDEM
                    .setNextStep(new ResolveDependency(this))
                    .setNextStep(new BuildProject(this))
                    .setNextStep(new TestProject(this))
                    .setNextStep(new GatherTestInformation(this, true, new BuildShouldFail(), false))
                    .setNextStep(new InitRepoToPush(this))
                    .setNextStep(new PushIncriminatedBuild(this))
                    .setNextStep(new ComputeClasspath(this, false))
                    .setNextStep(new ComputeSourceDir(this, false, false));

            for (String repairToolName : RepairnatorConfig.getInstance().getRepairTools()) {
                AbstractRepairStep repairStep = RepairToolsManager.getStepFromName(repairToolName);
                if (repairStep != null) {
                    repairStep.setProjectInspector(this);
                    lastStep = lastStep.setNextStep(repairStep);
                } else {
                    logger.error("Error while getting step class for following name: " + repairToolName);
                }
            }

            lastStep.setNextStep(new CommitPatch(this, false))
                    .setNextStep(new CheckoutPatchedBuild(this, true))
                    .setNextStep(new BuildProject(this))
                    .setNextStep(new TestProject(this))
                    .setNextStep(new GatherTestInformation(this, false, new BuildShouldPass(), true))
                    .setNextStep(new CommitPatch(this, true));

            cloneRepo.setDataSerializer(this.serializers);
            cloneRepo.setNotifiers(this.notifiers);

            try {
                cloneRepo.execute();
            } catch (Exception e) {
                this.jobStatus.addStepError("Unknown", e.getMessage());
                this.logger.error("Exception catch while executing steps: ", e);
                this.jobStatus.setFatalError(e);

                ErrorNotifier errorNotifier = ErrorNotifier.getInstance();
                if (errorNotifier != null) {
                    errorNotifier.observe(this);
                }

                for (AbstractDataSerializer serializer : this.serializers) {
                    serializer.serializeData(this);
                }
            }
        } else {
            this.logger.debug("Scanned build is not a failing build.");
        }
    }

    public CheckoutType getCheckoutType() {
        return checkoutType;
    }

    public void setCheckoutType(CheckoutType checkoutType) {
        this.checkoutType = checkoutType;
    }

    public List<AbstractNotifier> getNotifiers() {
        return notifiers;
    }
}

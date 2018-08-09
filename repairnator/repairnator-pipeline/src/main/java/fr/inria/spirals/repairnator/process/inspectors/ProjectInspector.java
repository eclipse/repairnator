package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.ErrorNotifier;
import fr.inria.spirals.repairnator.notifier.PatchNotifier;
import fr.inria.spirals.repairnator.pipeline.RepairToolsManager;
import fr.inria.spirals.repairnator.process.inspectors.properties.Properties;
import fr.inria.spirals.repairnator.process.inspectors.properties.machineInfo.MachineInfo;
import fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath;
import fr.inria.spirals.repairnator.process.step.paths.ComputeModules;
import fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir;
import fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir;
import fr.inria.spirals.repairnator.process.step.push.*;
import fr.inria.spirals.repairnator.process.step.repair.AbstractRepairStep;
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
import fr.inria.spirals.repairnator.states.ScannedBuildStatus;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class initialize the pipelines by creating the steps:
 * it's the backbone of the pipeline.
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
    private PatchNotifier patchNotifier;

    private CheckoutType checkoutType;

    private List<AbstractStep> steps;
    private AbstractStep finalStep;
    private boolean pipelineEnding;

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
        this.steps = new ArrayList<>();
        this.initProperties();
    }

    protected void initProperties() {
        try {
            Properties properties = this.jobStatus.getProperties();

            Build build = this.getBuggyBuild();
            long id = build.getId();
            String url = Utils.getTravisUrl(build.getId(), this.getRepoSlug());
            Date date = build.getFinishedAt();
            fr.inria.spirals.repairnator.process.inspectors.properties.builds.Build buggyBuild = new fr.inria.spirals.repairnator.process.inspectors.properties.builds.Build(id, url, date);
            properties.getBuilds().setBuggyBuild(buggyBuild);

            build = this.getPatchedBuild();
            if (build != null) {
                id = build.getId();
                url = Utils.getTravisUrl(build.getId(), this.getRepoSlug());
                date = build.getFinishedAt();
                fr.inria.spirals.repairnator.process.inspectors.properties.builds.Build patchedBuild = new fr.inria.spirals.repairnator.process.inspectors.properties.builds.Build(id, url, date);
                properties.getBuilds().setFixerBuild(patchedBuild);
            }

            MachineInfo machineInfo = properties.getReproductionBuggyBuild().getMachineInfo();
            machineInfo.setHostName(Utils.getHostname());

            fr.inria.spirals.repairnator.process.inspectors.properties.repository.Repository repository = properties.getRepository();
            repository.setName(this.getRepoSlug());
            repository.setUrl(Utils.getSimpleGithubRepoUrl(this.getRepoSlug()));

            if (this.getBuggyBuild().isPullRequest()) {
                repository.setIsPullRequest(true);
                repository.setPullRequestId(this.getBuggyBuild().getPullRequestNumber());
            }

            GitHub gitHub;
            try {
                gitHub = RepairnatorConfig.getInstance().getGithub();
                GHRepository repo = gitHub.getRepository(this.getRepoSlug());
                repository.setGithubId(repo.getId());
                if (repo.isFork()) {
                    repository.setIsFork(true);
                    repository.getOriginal().setName(repo.getParent().getFullName());
                    repository.getOriginal().setGithubId(repo.getParent().getId());
                    repository.getOriginal().setUrl(Utils.getSimpleGithubRepoUrl(repo.getParent().getFullName()));
                }
            } catch (IOException e) {
                this.logger.warn("It was not possible to retrieve information to check if " + this.getRepoSlug() + " is a fork.");
                this.logger.debug(e.toString());
            }

            switch (this.getBuildToBeInspected().getStatus()) {
                case ONLY_FAIL:
                    properties.setType("only_fail");
                    break;

                case FAILING_AND_PASSING:
                    properties.setType("failing_passing");
                    break;

                case PASSING_AND_PASSING_WITH_TEST_CHANGES:
                    properties.setType("passing_passing");
                    break;
            }
        } catch (Exception e) {
            this.logger.error("Error while initializing metrics.", e);
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
            cloneRepo
                    .addNextStep(new CheckoutBuggyBuild(this, true))
                    .addNextStep(new BuildProject(this))
                    .addNextStep(new TestProject(this))
                    .addNextStep(new GatherTestInformation(this, true, new BuildShouldFail(), false))
                    .addNextStep(new InitRepoToPush(this))
                    .addNextStep(new ComputeClasspath(this, false))
                    .addNextStep(new ComputeSourceDir(this, false, false))
                    .addNextStep(new ComputeTestDir(this, false));

            for (String repairToolName : RepairnatorConfig.getInstance().getRepairTools()) {
                AbstractRepairStep repairStep = RepairToolsManager.getStepFromName(repairToolName);
                if (repairStep != null) {
                    repairStep.setProjectInspector(this);
                    cloneRepo.addNextStep(repairStep);
                } else {
                    logger.error("Error while getting repair step class for following name: " + repairToolName);
                }
            }

            cloneRepo.addNextStep(new CommitPatch(this, CommitType.COMMIT_REPAIR_INFO))
                    .addNextStep(new CheckoutPatchedBuild(this, true))
                    .addNextStep(new BuildProject(this))
                    .addNextStep(new TestProject(this))
                    .addNextStep(new GatherTestInformation(this, true, new BuildShouldPass(), true))
                    .addNextStep(new CommitPatch(this, CommitType.COMMIT_HUMAN_PATCH));

            this.finalStep = new ComputeSourceDir(this, false, true); // this step is used to compute code metrics on the project


            this.finalStep.
                    addNextStep(new ComputeModules(this, false)).
                    addNextStep(new WritePropertyFile(this)).
                    addNextStep(new CommitProcessEnd(this)).
                    addNextStep(new PushProcessEnd(this));

            cloneRepo.setDataSerializer(this.serializers);
            cloneRepo.setNotifiers(this.notifiers);

            this.printPipeline();

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
            this.logger.debug("Build " + this.getBuggyBuild().getId() + " is not a failing build.");
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

    public PatchNotifier getPatchNotifier() {
        return patchNotifier;
    }

    public void setPatchNotifier(PatchNotifier patchNotifier) {
        this.patchNotifier = patchNotifier;
    }

    public AbstractStep getFinalStep() {
        return finalStep;
    }

    public void setFinalStep(AbstractStep finalStep) {
        this.finalStep = finalStep;
    }

    public boolean isPipelineEnding() {
        return pipelineEnding;
    }

    public void setPipelineEnding(boolean pipelineEnding) {
        this.pipelineEnding = pipelineEnding;
    }

    public void registerStep(AbstractStep step) {
        this.steps.add(this.steps.size(), step);
    }

    public List<AbstractStep> getSteps() {
        return steps;
    }

    public void printPipeline() {
        this.logger.info("----------------------------------------------------------------------");
        this.logger.info("PIPELINE STEPS");
        this.logger.info("----------------------------------------------------------------------");
        for (int i = 0; i < this.steps.size(); i++) {
            this.logger.info(this.steps.get(i).getName());
        }
    }

    public void printPipelineEnd() {
        this.logger.info("----------------------------------------------------------------------");
        this.logger.info("PIPELINE EXECUTION SUMMARY");
        this.logger.info("----------------------------------------------------------------------");
        int higherDuration = 0;
        for (int i = 0; i < this.steps.size(); i++) {
            AbstractStep step = this.steps.get(i);
            int stepDuration = step.getDuration();
            if (stepDuration > higherDuration) {
                higherDuration = stepDuration;
            }
        }
        for (int i = 0; i < this.steps.size(); i++) {
            AbstractStep step = this.steps.get(i);
            String stepName = step.getName();
            String stepStatus = (step.getStepStatus() != null) ? step.getStepStatus().getStatus().name() : "NOT RUN";
            String stepDuration = String.valueOf(step.getDuration());

            StringBuilder stepDurationFormatted = new StringBuilder();
            if (!stepStatus.equals("SKIPPED") && !stepStatus.equals("NOT RUN")) {
                stepDurationFormatted.append(" [ ");
                for (int j = 0; j < (String.valueOf(higherDuration).length() - stepDuration.length()); j++) {
                    stepDurationFormatted.append(" ");
                }
                stepDurationFormatted.append(stepDuration + " s ]");
            } else {
                for (int j = 0; j < (String.valueOf(higherDuration).length() + 7); j++) {
                    stepDurationFormatted.append(" ");
                }
            }

            int stringSize = stepName.length() + stepStatus.length() + stepDurationFormatted.length();
            int nbDot = 70 - stringSize;
            StringBuilder stepNameFormatted = new StringBuilder(stepName);
            for (int j = 0; j < nbDot; j++) {
                stepNameFormatted.append(".");
            }
            this.logger.info(stepNameFormatted + stepStatus + stepDurationFormatted);
        }
        String finding = AbstractDataSerializer.getPrettyPrintState(this).toUpperCase();
        finding = (finding.equals("UNKNOWN")) ? "-" : finding;
        this.logger.info("----------------------------------------------------------------------");
        this.logger.info("PIPELINE FINDING: "+finding);
        this.logger.info("----------------------------------------------------------------------");
    }

}

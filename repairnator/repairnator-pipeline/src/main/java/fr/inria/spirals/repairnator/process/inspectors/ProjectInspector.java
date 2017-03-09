package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.ScannedBuildStatus;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.config.RepairnatorConfigException;
import fr.inria.spirals.repairnator.process.step.*;
import fr.inria.spirals.repairnator.process.step.gatherinfocontract.BuildShouldFail;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * Created by urli on 26/12/2016.
 */
public class ProjectInspector {
    private final Logger logger = LoggerFactory.getLogger(ProjectInspector.class);

    private RepairnatorConfig config;
    private BuildToBeInspected buildToBeInspected;
    private String repoLocalPath;
    private ProjectState state;
    private String workspace;
    private String nopolSolverPath;
    private Map<String, Integer> stepsDurationsInSeconds;
    private GatherTestInformation testInformations;
    private PushIncriminatedBuild pushBuild;
    private NopolRepair nopolRepair;
    private boolean push;
    private Map<String, List<String>> stepErrors;
    private boolean autoclean;
    private String m2LocalPath;
    private List<AbstractDataSerializer> serializers;
    private List<URL> repairClassPath;
    private File[] repairSourceDir;
    private boolean isReproducedAsFail;
    private boolean isReproducedAsError;
    private boolean previousBuildFlag;

    public ProjectInspector(BuildToBeInspected buildToBeInspected, String workspace, List<AbstractDataSerializer> serializers) {
        try {
            this.config = RepairnatorConfig.getInstance();
        } catch (RepairnatorConfigException e) {
            throw new RuntimeException(e);
        }

        this.buildToBeInspected = buildToBeInspected;
        this.state = ProjectState.NONE;
        this.workspace = workspace;
        this.nopolSolverPath = config.getZ3solverPath();
        this.repoLocalPath = workspace + File.separator + getRepoSlug() + File.separator + buildToBeInspected.getBuild().getId();
        this.m2LocalPath = new File(this.repoLocalPath + File.separator + ".m2").getAbsolutePath();
        this.stepsDurationsInSeconds = new HashMap<String, Integer>();
        this.push = this.config.isPush();
        this.stepErrors = new HashMap<String, List<String>>();
        this.autoclean = this.config.isClean();
        this.serializers = serializers;
    }

    public List<AbstractDataSerializer> getSerializers() {
        return serializers;
    }

    public List<URL> getRepairClassPath() {
        return repairClassPath;
    }

    public void setRepairClassPath(List<URL> repairClassPath) {
        this.repairClassPath = repairClassPath;
    }

    public File[] getRepairSourceDir() {
        return repairSourceDir;
    }

    public void setRepairSourceDir(File[] repairSourceDir) {
        this.repairSourceDir = repairSourceDir;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getM2LocalPath() {
        return m2LocalPath;
    }

    public boolean isAutoclean() {
        return autoclean;
    }

    public void setAutoclean(boolean autoclean) {
        this.autoclean = autoclean;
    }

    public void setRepoLocalPath(String repoLocalPath) {
        this.repoLocalPath = repoLocalPath;
    }

    public String getNopolSolverPath() {
        return nopolSolverPath;
    }

    public ProjectState getState() {
        return state;
    }

    public void setState(ProjectState state) {
        this.state = state;
    }

    public BuildToBeInspected getBuildToBeInspected() {
        return this.buildToBeInspected;
    }

    public Build getBuild() {
        return this.buildToBeInspected.getBuild();
    }

    public Build getPreviousBuild() {
        return this.buildToBeInspected.getPreviousBuild();
    }

    public String getRepoSlug() {
        return this.buildToBeInspected.getBuild().getRepository().getSlug();
    }

    public String getRepoLocalPath() {
        return repoLocalPath;
    }

    public Map<String, Integer> getStepsDurationsInSeconds() {
        return this.stepsDurationsInSeconds;
    }

    public GatherTestInformation getTestInformations() {
        return testInformations;
    }

    public PushIncriminatedBuild getPushBuild() {
        return pushBuild;
    }

    public void setPushBuild(PushIncriminatedBuild pushBuild) {
        this.pushBuild = pushBuild;
    }

    public NopolRepair getNopolRepair() {
        return nopolRepair;
    }

    public String toString() {
        return this.getRepoLocalPath() + " : " + this.getState();
    }

    public boolean getPushMode() {
        return this.push;
    }

    public boolean isReproducedAsFail() {
        return isReproducedAsFail;
    }

    public void setReproducedAsFail(boolean reproducedAsFail) {
        isReproducedAsFail = reproducedAsFail;
    }

    public boolean isReproducedAsError() {
        return isReproducedAsError;
    }

    public void setReproducedAsError(boolean reproducedAsError) {
        isReproducedAsError = reproducedAsError;
    }

    public void addStepError(String step, String error) {
        if (!stepErrors.containsKey(step)) {
            stepErrors.put(step, new ArrayList<String>());
        }

        List<String> errors = stepErrors.get(step);
        errors.add(error);
    }

    public Map<String, List<String>> getStepErrors() {
        return stepErrors;
    }

    public void addStepDuration(String step, int duration) {
        this.stepsDurationsInSeconds.put(step, duration);
    }

    public void run() {
        if (this.buildToBeInspected.getStatus() == ScannedBuildStatus.ONLY_FAIL) {
            AbstractStep firstStep = null;

            this.testInformations = new GatherTestInformation(this, new BuildShouldFail());
            this.pushBuild = new PushIncriminatedBuild(this);
            this.pushBuild.setRemoteRepoUrl(PushIncriminatedBuild.REMOTE_REPO_REPAIR);

            this.nopolRepair = new NopolRepair(this);

            AbstractStep cloneRepo = new CloneRepository(this);
            AbstractStep checkoutBuild = new CheckoutBuild(this);
            AbstractStep buildRepo = new BuildProject(this);
            AbstractStep testProject = new TestProject(this);
            cloneRepo.setNextStep(checkoutBuild).setNextStep(buildRepo).setNextStep(testProject).setNextStep(this.testInformations);
            firstStep = cloneRepo;

            firstStep.setDataSerializer(this.serializers);

            if (push) {
                this.testInformations.setNextStep(this.pushBuild).setNextStep(new ComputeClasspath(this))
                        .setNextStep(new ComputeSourceDir(this)).setNextStep(this.nopolRepair);
            } else {
                this.logger.debug("Push boolean is set to false the failing builds won't be pushed.");
                this.testInformations.setNextStep(new ComputeClasspath(this)).setNextStep(new ComputeSourceDir(this))
                        .setNextStep(this.nopolRepair);
            }

            firstStep.setState(ProjectState.INIT);

            try {
                firstStep.execute();
            } catch (Exception e) {
                this.addStepError("Unknown", e.getMessage());
                this.logger.debug("Exception catch while executing steps: ", e);
            }
        } else {
            this.logger.debug("Scanned build is not a failing build.");
        }
    }

    public boolean isAboutAPreviousBuild() {
        return previousBuildFlag;
    }

    public void setPreviousBuildFlag(boolean previousBuildFlag) {
        this.previousBuildFlag = previousBuildFlag;
    }

    public void setTestInformations(GatherTestInformation testInformations) {
        this.testInformations = testInformations;
    }

}

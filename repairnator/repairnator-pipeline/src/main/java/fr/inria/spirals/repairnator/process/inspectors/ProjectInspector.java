package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.jtravis.entities.Build;
import fr.inria.spirals.repairnator.BuildToBeInspected;
import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.ScannedBuildStatus;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.config.RepairnatorConfigException;
import fr.inria.spirals.repairnator.process.nopol.NopolInformation;
import fr.inria.spirals.repairnator.process.step.*;
import fr.inria.spirals.repairnator.process.step.gatherinfocontract.BuildShouldFail;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
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
    private Map<String, List<String>> stepErrors;
    private boolean autoclean;
    private String m2LocalPath;
    private List<AbstractDataSerializer> serializers;
    private List<URL> repairClassPath;
    private File[] repairSourceDir;
    private List<NopolInformation> nopolInformations;
    private boolean isReproducedAsFail;
    private boolean isReproducedAsError;
    private boolean previousBuildFlag;
    private boolean hasBeenPushed;

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
        this.stepErrors = new HashMap<String, List<String>>();
        this.autoclean = this.config.isClean();
        this.serializers = serializers;
        this.hasBeenPushed = false;
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

    public String toString() {
        return this.getRepoLocalPath() + " : " + this.getState();
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

    public List<NopolInformation> getNopolInformations() {
        return nopolInformations;
    }

    public String getRemoteBranchName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("YYYYMMdd-HHmmss");
        String formattedDate = dateFormat.format(this.getBuild().getFinishedAt());

        return this.getRepoSlug().replace('/', '-') + '-' + this.getBuild().getId() + '-' + formattedDate;
    }

    public void setNopolInformations(List<NopolInformation> nopolInformations) {
        this.nopolInformations = nopolInformations;
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

    public boolean isHasBeenPushed() {
        return hasBeenPushed;
    }

    public void setHasBeenPushed(boolean hasBeenPushed) {
        this.hasBeenPushed = hasBeenPushed;
    }

    public void run() {
        if (this.buildToBeInspected.getStatus() == ScannedBuildStatus.ONLY_FAIL) {
            AbstractStep cloneRepo = new CloneRepository(this);
            cloneRepo.setNextStep(new CheckoutBuild(this))
                    .setNextStep(new BuildProject(this))
                    .setNextStep(new TestProject(this))
                    .setNextStep(new GatherTestInformation(this, new BuildShouldFail()))
                    .setNextStep(new PushIncriminatedBuild(this))
                    .setNextStep(new ComputeClasspath(this))
                    .setNextStep(new ComputeSourceDir(this))
                    .setNextStep(new NopolRepair(this));

            cloneRepo.setDataSerializer(this.serializers);
            cloneRepo.setState(ProjectState.INIT);

            try {
                cloneRepo.execute();
            } catch (Exception e) {
                this.addStepError("Unknown", e.getMessage());
                this.logger.error("Exception catch while executing steps: ", e);
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

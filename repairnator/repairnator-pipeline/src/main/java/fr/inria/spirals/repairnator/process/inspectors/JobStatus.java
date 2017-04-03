package fr.inria.spirals.repairnator.process.inspectors;

import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.process.nopol.NopolInformation;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by urli on 23/03/2017.
 */
public class JobStatus {
    private ProjectState state;
    private List<URL> repairClassPath;
    private File[] repairSourceDir;
    private List<NopolInformation> nopolInformations;
    private boolean isReproducedAsFail;
    private boolean isReproducedAsError;
    private String pomDirPath;
    private boolean hasBeenPushed;
    private Map<String, Integer> stepsDurationsInSeconds;
    private Map<String, List<String>> stepErrors;
    private String failingModulePath;
    private Collection<FailureLocation> failureLocations;
    private Set<String> failureNames;
    private String gitBranchUrl;

    public JobStatus(String pomDirPath) {
        this.state = ProjectState.NONE;
        this.stepsDurationsInSeconds = new HashMap<>();
        this.stepErrors = new HashMap<>();
        this.pomDirPath = pomDirPath;
    }

    public ProjectState getState() {
        return state;
    }

    public void setState(ProjectState state) {
        this.state = state;
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

    public List<NopolInformation> getNopolInformations() {
        return nopolInformations;
    }

    public void setNopolInformations(List<NopolInformation> nopolInformations) {
        this.nopolInformations = nopolInformations;
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

    public String getPomDirPath() {
        return pomDirPath;
    }

    public void setPomDirPath(String pomDirPath) {
        this.pomDirPath = pomDirPath;
    }

    public void addStepError(String step, String error) {
        if (!stepErrors.containsKey(step)) {
            stepErrors.put(step, new ArrayList<String>());
        }

        List<String> errors = stepErrors.get(step);
        errors.add(error);
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

    public Map<String, Integer> getStepsDurationsInSeconds() {
        return stepsDurationsInSeconds;
    }

    public Map<String, List<String>> getStepErrors() {
        return stepErrors;
    }

    public String getFailingModulePath() {
        return failingModulePath;
    }

    public void setFailingModulePath(String failingModulePath) {
        this.failingModulePath = failingModulePath;
    }

    public Collection<FailureLocation> getFailureLocations() {
        return failureLocations;
    }

    public void setFailureLocations(Collection<FailureLocation> failureLocations) {
        this.failureLocations = failureLocations;
    }

    public Set<String> getFailureNames() {
        return failureNames;
    }

    public void setFailureNames(Set<String> failureNames) {
        this.failureNames = failureNames;
    }

    public String getGitBranchUrl() {
        return gitBranchUrl;
    }

    public void setGitBranchUrl(String gitBranchUrl) {
        this.gitBranchUrl = gitBranchUrl;
    }
}

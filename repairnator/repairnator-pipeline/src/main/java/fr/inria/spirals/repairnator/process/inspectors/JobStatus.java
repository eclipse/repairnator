package fr.inria.spirals.repairnator.process.inspectors;

import com.google.gson.JsonElement;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import fr.inria.spirals.repairnator.states.PushState;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by urli on 23/03/2017.
 */
public class JobStatus {
    private PushState pushState;
    private List<URL> repairClassPath;

    private File[] repairSourceDir;
    private File[] testDir;

    /**
     * List of patches indexed by the name of the tool to produce them
     */
    private Map<String, List<RepairPatch>> listOfPatches;

    /**
     * Diagnostic about repair tool on the form of a JsonElement
     */
    private Map<String, JsonElement> toolDiagnostic;

    private boolean isReproducedAsFail;
    private String pomDirPath;
    private boolean hasBeenPushed;

    private Collection<FailureLocation> failureLocations;
    private String failingModulePath;
    private Map<String, List<String>> stepErrors;
    private String gitBranchUrl;
    private boolean hasBeenPatched;
    private Throwable fatalError;

    private Metrics metrics;

    private List<String> createdFilesToPush;
    private boolean hasBeenForked;
    private String forkURL;

    private List<StepStatus> stepStatuses;

    public JobStatus(String pomDirPath) {
        this.stepErrors = new HashMap<>();
        this.pomDirPath = pomDirPath;
        this.repairSourceDir = new File[]{new File("src/main/java")};
        this.failingModulePath = pomDirPath;
        this.metrics = new Metrics();
        this.createdFilesToPush = new ArrayList<>();
        this.stepStatuses = new ArrayList<>();
        this.listOfPatches = new HashMap<>();
        this.toolDiagnostic = new HashMap<>();
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

    public boolean isReproducedAsFail() {
        return isReproducedAsFail;
    }

    public void setReproducedAsFail(boolean reproducedAsFail) {
        isReproducedAsFail = reproducedAsFail;
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


    public boolean isHasBeenPushed() {
        return hasBeenPushed;
    }

    public void setHasBeenPushed(boolean hasBeenPushed) {
        this.hasBeenPushed = hasBeenPushed;
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

    public String getGitBranchUrl() {
        return gitBranchUrl;
    }

    public void setGitBranchUrl(String gitBranchUrl) {
        this.gitBranchUrl = gitBranchUrl;
    }

    public boolean isHasBeenPatched() {
        return hasBeenPatched;
    }

    public void setHasBeenPatched(boolean hasBeenPatched) {
        this.hasBeenPatched = hasBeenPatched;
    }

    public PushState getPushState() {
        return pushState;
    }

    public void setPushState(PushState pushState) {
        this.pushState = pushState;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public File[] getTestDir() {
        return testDir;
    }

    public void setTestDir(File[] testDir) {
        this.testDir = testDir;
    }

    public Throwable getFatalError() {
        return fatalError;
    }

    public void setFatalError(Throwable fatalError) {
        this.fatalError = fatalError;
    }

    public void addFileToPush(String filePath) {
        if (!this.createdFilesToPush.contains(filePath)) {
            this.createdFilesToPush.add(filePath);
        }
    }

    public List<String> getCreatedFilesToPush() {
        return createdFilesToPush;
    }

    public void setHasBeenForked(boolean hasBeenForked) {
        this.hasBeenForked = hasBeenForked;
    }

    public boolean isHasBeenForked() {
        return hasBeenForked;
    }

    public String getForkURL() {
        return forkURL;
    }

    public void setForkURL(String forkURL) {
        this.forkURL = forkURL;
    }

    public void addStepStatus(StepStatus stepStatus) {
        this.stepStatuses.add(stepStatus);
    }

    public void addToolDiagnostic(String toolName, JsonElement diagnostic) {
        this.toolDiagnostic.put(toolName, diagnostic);
    }

    public void addPatches(String toolName, List<RepairPatch> patches) {
        this.listOfPatches.put(toolName, patches);
    }

    public Map<String, List<RepairPatch>> getListOfPatches() {
        return listOfPatches;
    }

    public Map<String, JsonElement> getToolDiagnostic() {
        return toolDiagnostic;
    }
}

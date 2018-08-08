package fr.inria.spirals.repairnator.process.inspectors;

import com.google.gson.JsonElement;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.properties.Properties;
import fr.inria.spirals.repairnator.process.testinformation.FailureLocation;
import fr.inria.spirals.repairnator.states.LauncherMode;
import fr.inria.spirals.repairnator.states.PushState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * This class contains all information about the status of the pipeline
 */
public class JobStatus {
    private final Logger logger = LoggerFactory.getLogger(JobStatus.class);

    private List<PushState> pushStates;
    private List<URL> repairClassPath;

    private File[] repairSourceDir;
    private File[] testDir;
    private File[] modules;

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

    private Set<FailureLocation> failureLocations;
    private String failingModulePath;
    private Map<String, List<String>> stepErrors;
    private String gitBranchUrl;
    private boolean hasBeenPatched;
    private Throwable fatalError;

    private Metrics metrics;
    private java.util.Properties properties4repairnator;
    private Properties properties;

    private List<String> createdFilesToPush;
    private boolean hasBeenForked;
    private String forkURL;

    private List<StepStatus> stepStatuses;

    private List<String> PRCreated;

    public JobStatus(String pomDirPath) {
        this.stepErrors = new HashMap<>();
        this.pomDirPath = pomDirPath;
        this.repairSourceDir = new File[]{new File("src/main/java")};
        this.failingModulePath = pomDirPath;
        this.metrics = new Metrics();
        this.properties4repairnator = new java.util.Properties();
        this.properties = new Properties();
        this.createdFilesToPush = new ArrayList<>();
        this.stepStatuses = new ArrayList<>();
        this.pushStates = new ArrayList<>();
        this.listOfPatches = new HashMap<>();
        this.toolDiagnostic = new HashMap<>();
        this.repairClassPath = new ArrayList<>();
        this.PRCreated = new ArrayList<>();
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
        this.writeProperty("failingModule", this.failingModulePath);
        this.properties.getTests().setFailingModule(this.failingModulePath);
    }

    public Set<FailureLocation> getFailureLocations() {
        return failureLocations;
    }

    public void setFailureLocations(Set<FailureLocation> failureLocations) {
        this.failureLocations = failureLocations;
        this.writeProperty("failing-test-cases", this.failureLocations);
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

    public List<PushState> getPushStates() {
        return pushStates;
    }

    public PushState getLastPushState() {
        return pushStates.get(pushStates.size() - 1);
    }

    public void addPushState(PushState pushState) {
        this.pushStates.add(pushState);
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public java.util.Properties getProperties4Repairnator() {
        return properties4repairnator;
    }

    public void writeProperty(String propertyName, Object value) {
        if (RepairnatorConfig.getInstance().getLauncherMode() == LauncherMode.REPAIR) {
            if (value != null) {
                this.properties4repairnator.put(propertyName, value);
            } else {
                this.logger.warn("Trying to write null value for property: " + propertyName);
            }
        }
    }

    public Properties getProperties() {
        return properties;
    }

    public File[] getTestDir() {
        return testDir;
    }

    public void setTestDir(File[] testDir) {
        this.testDir = testDir;
    }

    public File[] getModules() {
        return modules;
    }

    public void setModules(File[] modules) {
        this.modules = modules;
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

    public boolean isCreatedFileToPush(String filePath) {
        for (String createdFileToPush : this.createdFilesToPush) {
            if (filePath.endsWith(createdFileToPush)) {
                return true;
            }
        }
        return false;
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

    public List<StepStatus> getStepStatuses() {
        return stepStatuses;
    }

    public void addPatches(String toolName, List<RepairPatch> patches) {
        this.listOfPatches.put(toolName, patches);
    }

    public List<RepairPatch> getAllPatches() {
        List<RepairPatch> allPatches = new ArrayList<>();
        for (List<RepairPatch> repairPatches : this.listOfPatches.values()) {
            allPatches.addAll(repairPatches);
        }
        return allPatches;
    }

    public Map<String, List<RepairPatch>> getListOfPatches() {
        return listOfPatches;
    }

    public Map<String, JsonElement> getToolDiagnostic() {
        return toolDiagnostic;
    }

    public List<String> getPRCreated() {
        return PRCreated;
    }

    public void addPRCreated(String prURL) {
        this.PRCreated.add(prURL);
    }
}

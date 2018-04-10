package fr.inria.spirals.repairnator.process.inspectors;


import eu.stamp.project.assertfixer.AssertFixerResult;
import fr.inria.main.AstorOutputStatus;
import com.google.gson.JsonElement;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.process.nopol.NopolInformation;
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
    private PipelineState pipelineState;
    private PushState pushState;
    private List<URL> repairClassPath;

    private File[] repairSourceDir;
    private File[] testDir;

    private List<NopolInformation> nopolInformations;
    private List<String> nopolPatches;
    private List<String> npeFixPatches;

    private JsonElement astorResults;
    private List<String> astorPatches;
    private AstorOutputStatus astorStatus;

    private JsonElement npeFixResults;

    private List<AssertFixerResult> assertFixerResults;

    private boolean isReproducedAsFail;
    private boolean isReproducedAsError;
    private String pomDirPath;
    private boolean hasBeenPushed;

    private Collection<FailureLocation> failureLocations;
    private String failingModulePath;
    private Map<String, List<String>> stepErrors;
    private String gitBranchUrl;
    private boolean hasBeenPatched;

    private boolean commitRetrievedFromGithub;
    private Throwable fatalError;

    private Metrics metrics;

    private List<String> createdFilesToPush;
    private boolean hasBeenForked;
    private String forkURL;

    public JobStatus(String pomDirPath) {
        this.pipelineState = PipelineState.NONE;
        this.stepErrors = new HashMap<>();
        this.pomDirPath = pomDirPath;
        this.repairSourceDir = new File[]{new File("src/main/java")};
        this.failingModulePath = pomDirPath;
        this.metrics = new Metrics();
        this.createdFilesToPush = new ArrayList<>();
        this.nopolPatches = new ArrayList<>();
        this.astorPatches = new ArrayList<>();
        this.npeFixPatches = new ArrayList<>();
    }

    public PipelineState getPipelineState() {
        return pipelineState;
    }

    public void setPipelineState(PipelineState pipelineState) {
        this.pipelineState = pipelineState;
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

    public List<String> getNpeFixPatches() {
        return npeFixPatches;
    }

    public void setNpeFixPatches(List<String> npeFixPatches) {
        this.npeFixPatches = npeFixPatches;
    }

    public List<String> getAstorPatches() {
        return astorPatches;
    }

    public void setAstorPatches(List<String> astorPatches) {
        this.astorPatches = astorPatches;
    }

    public AstorOutputStatus getAstorStatus() {
        return astorStatus;
    }

    public void setAstorStatus(AstorOutputStatus astorStatus) {
        this.astorStatus = astorStatus;
    }

    public boolean isCommitRetrievedFromGithub() {
        return commitRetrievedFromGithub;
    }

    public void setCommitRetrievedFromGithub(boolean commitRetrievedFromGithub) {
        this.commitRetrievedFromGithub = commitRetrievedFromGithub;
    }

    public Throwable getFatalError() {
        return fatalError;
    }

    public void setFatalError(Throwable fatalError) {
        this.fatalError = fatalError;
    }

    public JsonElement getNpeFixResults() {
        return npeFixResults;
    }

    public void setNpeFixResults(JsonElement npeFixResults) {
        this.npeFixResults = npeFixResults;
    }

    public JsonElement getAstorResults() {
        return astorResults;
    }

    public void setAstorResults(JsonElement astorResults) {
        this.astorResults = astorResults;
    }

    public void addFileToPush(String filePath) {
        if (!this.createdFilesToPush.contains(filePath)) {
            this.createdFilesToPush.add(filePath);
        }
    }

    public List<String> getCreatedFilesToPush() {
        return createdFilesToPush;
    }

    public List<String> getNopolPatches() {
        return nopolPatches;
    }

    public void setNopolPatches(List<String> nopolPatches) {
        this.nopolPatches = nopolPatches;
    }

    public boolean isHasBeenForked() {
        return hasBeenForked;
    }

    public void setHasBeenForked(boolean hasBeenForked) {
        this.hasBeenForked = hasBeenForked;
    }

    public String getForkURL() {
        return forkURL;
    }

    public void setForkURL(String forkURL) {
        this.forkURL = forkURL;
    }

    public List<AssertFixerResult> getAssertFixerResults() {
        return assertFixerResults;
    }

    public void setAssertFixerResults(List<AssertFixerResult> assertFixerResults) {
        this.assertFixerResults = assertFixerResults;
    }
}

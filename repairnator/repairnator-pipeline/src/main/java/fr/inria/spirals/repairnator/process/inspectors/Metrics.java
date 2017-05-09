package fr.inria.spirals.repairnator.process.inspectors;

import com.google.gson.JsonElement;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by urli on 28/04/2017.
 */
@XmlRootElement
public class Metrics {
    private Map<String, Integer> stepsDurationsInSeconds;
    private Set<String> failureNames;

    private Date reproductionDate;

    private int nbFailingTests;
    private int nbRunningTests;

    private JsonElement sizeProjectLOC;
    private int nbFileApp;
    private int nbFileTests;

    private int nbLibraries;
    private int nbCPU;
    private Map<String, Long> freeMemoryByStep;
    private long freeMemory;
    private long totalMemory;

    private int buggyBuildId;
    private int patchedBuilId;

    private String buggyBuildURL;
    private String patchedBuildURL;

    private Date buggyBuildDate;
    private Date patchedBuildDate;

    private int patchAddedLines;
    private int patchDeletedLines;
    private int patchChangedFiles;

    private Map<String, Integer> angelicValuesByTest;

    private String bugCommit;
    private String bugCommitUrl;
    private boolean reconstructedBugCommit;

    private String patchCommit;
    private String patchCommitUrl;

    public Metrics() {
        this.reproductionDate = new Date();
        this.stepsDurationsInSeconds = new HashMap<>();
        this.angelicValuesByTest = new HashMap<>();
        this.freeMemoryByStep = new HashMap<>();
    }

    public Date getReproductionDate() {
        return reproductionDate;
    }

    public void addStepDuration(String step, int duration) {
        this.stepsDurationsInSeconds.put(step, duration);
    }

    public void setStepsDurationsInSeconds(Map<String, Integer> stepsDurationsInSeconds) {
        this.stepsDurationsInSeconds = stepsDurationsInSeconds;
    }

    public Map<String, Integer> getStepsDurationsInSeconds() {
        return stepsDurationsInSeconds;
    }

    public Set<String> getFailureNames() {
        return failureNames;
    }

    public void setFailureNames(Set<String> failureNames) {
        this.failureNames = failureNames;
    }

    public int getNbFailingTests() {
        return nbFailingTests;
    }

    public void setNbFailingTests(int nbFailingTests) {
        this.nbFailingTests = nbFailingTests;
    }

    public int getNbRunningTests() {
        return nbRunningTests;
    }

    public void setNbRunningTests(int nbRunningTests) {
        this.nbRunningTests = nbRunningTests;
    }

    public JsonElement getSizeProjectLOC() {
        return sizeProjectLOC;
    }

    public void setSizeProjectLOC(JsonElement sizeProjectLOC) {
        this.sizeProjectLOC = sizeProjectLOC;
    }

    public int getNbFileApp() {
        return nbFileApp;
    }

    public void setNbFileApp(int nbFileApp) {
        this.nbFileApp = nbFileApp;
    }

    public int getNbFileTests() {
        return nbFileTests;
    }

    public void setNbFileTests(int nbFileTests) {
        this.nbFileTests = nbFileTests;
    }

    public int getNbLibraries() {
        return nbLibraries;
    }

    public void setNbLibraries(int nbLibraries) {
        this.nbLibraries = nbLibraries;
    }

    public int getNbCPU() {
        return nbCPU;
    }

    public void setNbCPU(int nbCPU) {
        this.nbCPU = nbCPU;
    }

    public long getFreeMemory() {
        return freeMemory;
    }

    public void setFreeMemory(long freeMemory) {
        this.freeMemory = freeMemory;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public int getPatchAddedLines() {
        return patchAddedLines;
    }

    public void setPatchAddedLines(int patchAddedLines) {
        this.patchAddedLines = patchAddedLines;
    }

    public int getPatchDeletedLines() {
        return patchDeletedLines;
    }

    public void setPatchDeletedLines(int patchDeletedLines) {
        this.patchDeletedLines = patchDeletedLines;
    }

    public Map<String, Integer> getAngelicValuesByTest() {
        return angelicValuesByTest;
    }

    public void setAngelicValuesByTest(Map<String, Integer> angelicValuesByTest) {
        this.angelicValuesByTest = angelicValuesByTest;
    }

    public String getBugCommit() {
        return bugCommit;
    }

    public void setBugCommit(String bugCommit) {
        this.bugCommit = bugCommit;
    }

    public String getBugCommitUrl() {
        return bugCommitUrl;
    }

    public void setBugCommitUrl(String bugCommitUrl) {
        this.bugCommitUrl = bugCommitUrl;
    }

    public boolean isReconstructedBugCommit() {
        return reconstructedBugCommit;
    }

    public void setReconstructedBugCommit(boolean reconstructedBugCommit) {
        this.reconstructedBugCommit = reconstructedBugCommit;
    }

    public String getPatchCommit() {
        return patchCommit;
    }

    public void setPatchCommit(String patchCommit) {
        this.patchCommit = patchCommit;
    }

    public String getPatchCommitUrl() {
        return patchCommitUrl;
    }

    public void setPatchCommitUrl(String patchCommitUrl) {
        this.patchCommitUrl = patchCommitUrl;
    }

    public Map<String, Long> getFreeMemoryByStep() {
        return freeMemoryByStep;
    }

    public void addFreeMemoryByStep(String step, long value) {
        this.freeMemoryByStep.put(step, value);
    }

    public void addAngelicValueByTest(String test, int nbAngelicValue) {
        this.angelicValuesByTest.put(test, nbAngelicValue);
    }

    public int getPatchChangedFiles() {
        return patchChangedFiles;
    }

    public void setPatchChangedFiles(int patchChangedFiles) {
        this.patchChangedFiles = patchChangedFiles;
    }

    public int getBuggyBuildId() {
        return buggyBuildId;
    }

    public void setBuggyBuildId(int buggyBuildId) {
        this.buggyBuildId = buggyBuildId;
    }

    public int getPatchedBuilId() {
        return patchedBuilId;
    }

    public void setPatchedBuilId(int patchedBuilId) {
        this.patchedBuilId = patchedBuilId;
    }

    public String getBuggyBuildURL() {
        return buggyBuildURL;
    }

    public void setBuggyBuildURL(String buggyBuildURL) {
        this.buggyBuildURL = buggyBuildURL;
    }

    public String getPatchedBuildURL() {
        return patchedBuildURL;
    }

    public void setPatchedBuildURL(String patchedBuildURL) {
        this.patchedBuildURL = patchedBuildURL;
    }

    public Date getBuggyBuildDate() {
        return buggyBuildDate;
    }

    public void setBuggyBuildDate(Date buggyBuildDate) {
        this.buggyBuildDate = buggyBuildDate;
    }

    public Date getPatchedBuildDate() {
        return patchedBuildDate;
    }

    public void setPatchedBuildDate(Date patchedBuildDate) {
        this.patchedBuildDate = patchedBuildDate;
    }
}

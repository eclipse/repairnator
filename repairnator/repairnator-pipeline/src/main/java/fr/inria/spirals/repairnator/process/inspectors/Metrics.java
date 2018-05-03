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
    private Date reproductionDate;

    private int buggyBuildId;
    private int patchedBuilId;

    private String buggyBuildURL;
    private String patchedBuildURL;

    private Date buggyBuildDate;
    private Date patchedBuildDate;

    private String bugCommit;
    private String bugCommitUrl;

    private String patchCommit;
    private String patchCommitUrl;

    private int patchChangedFiles;

    private int patchAddedLines;
    private int patchDeletedLines;

    private int nbFailingTests;
    private int nbErroringTests;
    private int nbSkippingTests;
    private int nbRunningTests;

    private Set<String> failureNames;

    private int nbLibraries;
    private int nbFileApp;
    private int nbFileTests;

    private JsonElement sizeProjectLOC;

    private int nbCPU;
    private long freeMemory;
    private long totalMemory;

    private Map<String, Integer> angelicValuesByTest;

    private boolean reconstructedBugCommit;

    private Map<String, Integer> stepsDurationsInSeconds;
    private Map<String, Long> freeMemoryByStep;

    public Metrics() {
        this.reproductionDate = new Date();
        this.stepsDurationsInSeconds = new HashMap<>();
        this.angelicValuesByTest = new HashMap<>();
        this.freeMemoryByStep = new HashMap<>();
    }

    public void addStepDuration(String step, int duration) {
        this.stepsDurationsInSeconds.put(step, duration);
    }

    public void setStepsDurationsInSeconds(Map<String, Integer> stepsDurationsInSeconds) {
        this.stepsDurationsInSeconds = stepsDurationsInSeconds;
    }

    public void setFailureNames(Set<String> failureNames) {
        this.failureNames = failureNames;
    }

    public void setNbFailingTests(int nbFailingTests) {
        this.nbFailingTests = nbFailingTests;
    }

    public int getNbErroringTests() {
        return nbErroringTests;
    }

    public void setNbErroringTests(int nbErroringTests) {
        this.nbErroringTests = nbErroringTests;
    }

    public int getNbSkippingTests() {
        return nbSkippingTests;
    }

    public void setNbSkippingTests(int nbSkippingTests) {
        this.nbSkippingTests = nbSkippingTests;
    }

    public void setNbRunningTests(int nbRunningTests) {
        this.nbRunningTests = nbRunningTests;
    }

    public void setSizeProjectLOC(JsonElement sizeProjectLOC) {
        this.sizeProjectLOC = sizeProjectLOC;
    }

    public void setNbFileApp(int nbFileApp) {
        this.nbFileApp = nbFileApp;
    }

    public void setNbFileTests(int nbFileTests) {
        this.nbFileTests = nbFileTests;
    }

    public void setNbLibraries(int nbLibraries) {
        this.nbLibraries = nbLibraries;
    }

    public void setNbCPU(int nbCPU) {
        this.nbCPU = nbCPU;
    }

    public void setFreeMemory(long freeMemory) {
        this.freeMemory = freeMemory;
    }

    public void setTotalMemory(long totalMemory) {
        this.totalMemory = totalMemory;
    }

    public void setPatchAddedLines(int patchAddedLines) {
        this.patchAddedLines = patchAddedLines;
    }

    public void setPatchDeletedLines(int patchDeletedLines) {
        this.patchDeletedLines = patchDeletedLines;
    }

    public void setBugCommit(String bugCommit) {
        this.bugCommit = bugCommit;
    }

    public void setBugCommitUrl(String bugCommitUrl) {
        this.bugCommitUrl = bugCommitUrl;
    }

    public void setReconstructedBugCommit(boolean reconstructedBugCommit) {
        this.reconstructedBugCommit = reconstructedBugCommit;
    }

    public void setPatchCommit(String patchCommit) {
        this.patchCommit = patchCommit;
    }

    public void setPatchCommitUrl(String patchCommitUrl) {
        this.patchCommitUrl = patchCommitUrl;
    }

    public void addFreeMemoryByStep(String step, long value) {
        this.freeMemoryByStep.put(step, value);
    }

    public void addAngelicValueByTest(String test, int nbAngelicValue) {
        this.angelicValuesByTest.put(test, nbAngelicValue);
    }

    public void setPatchChangedFiles(int patchChangedFiles) {
        this.patchChangedFiles = patchChangedFiles;
    }

    public void setBuggyBuildId(int buggyBuildId) {
        this.buggyBuildId = buggyBuildId;
    }

    public void setPatchedBuilId(int patchedBuilId) {
        this.patchedBuilId = patchedBuilId;
    }

    public void setBuggyBuildURL(String buggyBuildURL) {
        this.buggyBuildURL = buggyBuildURL;
    }

    public void setPatchedBuildURL(String patchedBuildURL) {
        this.patchedBuildURL = patchedBuildURL;
    }

    public void setBuggyBuildDate(Date buggyBuildDate) {
        this.buggyBuildDate = buggyBuildDate;
    }

    public void setPatchedBuildDate(Date patchedBuildDate) {
        this.patchedBuildDate = patchedBuildDate;
    }

    public Date getReproductionDate() {
        return reproductionDate;
    }

    public int getBuggyBuildId() {
        return buggyBuildId;
    }

    public int getPatchedBuilId() {
        return patchedBuilId;
    }

    public String getBuggyBuildURL() {
        return buggyBuildURL;
    }

    public String getPatchedBuildURL() {
        return patchedBuildURL;
    }

    public Date getBuggyBuildDate() {
        return buggyBuildDate;
    }

    public Date getPatchedBuildDate() {
        return patchedBuildDate;
    }

    public String getBugCommit() {
        return bugCommit;
    }

    public String getBugCommitUrl() {
        return bugCommitUrl;
    }

    public String getPatchCommit() {
        return patchCommit;
    }

    public String getPatchCommitUrl() {
        return patchCommitUrl;
    }

    public int getPatchChangedFiles() {
        return patchChangedFiles;
    }

    public int getPatchAddedLines() {
        return patchAddedLines;
    }

    public int getPatchDeletedLines() {
        return patchDeletedLines;
    }

    public int getNbFailingTests() {
        return nbFailingTests;
    }

    public int getNbRunningTests() {
        return nbRunningTests;
    }

    public Set<String> getFailureNames() {
        return failureNames;
    }

    public int getNbLibraries() {
        return nbLibraries;
    }

    public int getNbFileApp() {
        return nbFileApp;
    }

    public int getNbFileTests() {
        return nbFileTests;
    }

    public JsonElement getSizeProjectLOC() {
        return sizeProjectLOC;
    }

    public int getNbCPU() {
        return nbCPU;
    }

    public long getFreeMemory() {
        return freeMemory;
    }

    public long getTotalMemory() {
        return totalMemory;
    }

    public Map<String, Integer> getAngelicValuesByTest() {
        return angelicValuesByTest;
    }

    public boolean isReconstructedBugCommit() {
        return reconstructedBugCommit;
    }

    public Map<String, Integer> getStepsDurationsInSeconds() {
        return stepsDurationsInSeconds;
    }

    public Map<String, Long> getFreeMemoryByStep() {
        return freeMemoryByStep;
    }
}

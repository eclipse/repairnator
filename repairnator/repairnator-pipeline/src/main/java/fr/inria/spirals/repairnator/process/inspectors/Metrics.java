package fr.inria.spirals.repairnator.process.inspectors;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by urli on 28/04/2017.
 */
public class Metrics {
    private Map<String, Integer> stepsDurationsInSeconds;
    private Set<String> failureNames;

    private int nbFailingTests;
    private int nbRunningTests;

    private int sizeProjectLOC;
    private int nbFileApp;
    private int nbFileTests;

    private int nbLibraries;
    private int nbCPU;
    private int freeMemory;
    private int totalMemory;

    private int patchAddedLines;
    private int patchRemovedLines;
    private int patchModifiesLines;
    private int patchModifiedFiles;

    private Map<String, Integer> angelicValuesByTest;
    private Map<String, Integer> executedLinesByTest;

    private String bugCommit;
    private String bugCommitUrl;
    private boolean reconstructedBugCommit;

    private String patchCommit;
    private String patchCommitUrl;

    public Metrics() {
        this.stepsDurationsInSeconds = new HashMap<>();
        this.angelicValuesByTest = new HashMap<>();
        this.executedLinesByTest = new HashMap<>();
    }


    public void addStepDuration(String step, int duration) {
        this.stepsDurationsInSeconds.put(step, duration);
    }

    public void setStepsDurationsInSeconds(Map<String, Integer> stepsDurationsInSeconds) {
        this.stepsDurationsInSeconds = stepsDurationsInSeconds;
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

    public int getSizeProjectLOC() {
        return sizeProjectLOC;
    }

    public void setSizeProjectLOC(int sizeProjectLOC) {
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

    public int getFreeMemory() {
        return freeMemory;
    }

    public void setFreeMemory(int freeMemory) {
        this.freeMemory = freeMemory;
    }

    public int getTotalMemory() {
        return totalMemory;
    }

    public void setTotalMemory(int totalMemory) {
        this.totalMemory = totalMemory;
    }

    public int getPatchAddedLines() {
        return patchAddedLines;
    }

    public void setPatchAddedLines(int patchAddedLines) {
        this.patchAddedLines = patchAddedLines;
    }

    public int getPatchRemovedLines() {
        return patchRemovedLines;
    }

    public void setPatchRemovedLines(int patchRemovedLines) {
        this.patchRemovedLines = patchRemovedLines;
    }

    public int getPatchModifiesLines() {
        return patchModifiesLines;
    }

    public void setPatchModifiesLines(int patchModifiesLines) {
        this.patchModifiesLines = patchModifiesLines;
    }

    public int getPatchModifiedFiles() {
        return patchModifiedFiles;
    }

    public void setPatchModifiedFiles(int patchModifiedFiles) {
        this.patchModifiedFiles = patchModifiedFiles;
    }

    public Map<String, Integer> getAngelicValuesByTest() {
        return angelicValuesByTest;
    }

    public void setAngelicValuesByTest(Map<String, Integer> angelicValuesByTest) {
        this.angelicValuesByTest = angelicValuesByTest;
    }

    public Map<String, Integer> getExecutedLinesByTest() {
        return executedLinesByTest;
    }

    public void setExecutedLinesByTest(Map<String, Integer> executedLinesByTest) {
        this.executedLinesByTest = executedLinesByTest;
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
}

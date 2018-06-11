package fr.inria.spirals.repairnator.process.inspectors.metrics4bears.projectMetrics;

public class ProjectMetrics {

    private int numberSourceFiles;
    private int numberTestFiles;
    private int numberLibraries;

    public ProjectMetrics() {}

    public int getNumberSourceFiles() {
        return numberSourceFiles;
    }

    public void setNumberSourceFiles(int numberSourceFiles) {
        this.numberSourceFiles = numberSourceFiles;
    }

    public void setNumberTestFiles(int numberTestFiles) {
        this.numberTestFiles = numberTestFiles;
    }

    public int getNumberTestFiles() {
        return numberTestFiles;
    }

    public int getNumberLibraries() {
        return numberLibraries;
    }

    public void setNumberLibraries(int numberLibraries) {
        this.numberLibraries = numberLibraries;
    }

}

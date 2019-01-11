package fr.inria.spirals.repairnator.process.inspectors.properties.projectMetrics;

public class ProjectMetrics {

    /**
     * {@code numberModules} is calculated in the step
     * {@link fr.inria.spirals.repairnator.process.step.paths.ComputeModules}.
     */
    private int numberModules;
    /**
     * {@code numberPlugins} is calculated in the step
     * {@link fr.inria.spirals.repairnator.process.step.paths.ComputePlugins}.
     */
    private int numberPlugins;
    /**
     * {@code numberSourceFiles} is calculated in the step
     * {@link fr.inria.spirals.repairnator.process.step.paths.ComputeSourceDir}.
     */
    private int numberSourceFiles;
    /**
     * {@code numberTestFiles} is calculated in the step
     * {@link fr.inria.spirals.repairnator.process.step.paths.ComputeTestDir}.
     */
    private int numberTestFiles;
    /**
     * {@code numberLibrariesFailingModule} is calculated in the step
     * {@link fr.inria.spirals.repairnator.process.step.paths.ComputeClasspath}.
     */
    private int numberLibrariesFailingModule;

    private int numberLines;

    public ProjectMetrics() {}

    public int getNumberModules() {
        return numberModules;
    }

    public void setNumberModules(int numberModules) {
        this.numberModules = numberModules;
    }

    public int getNumberPlugins() {
        return numberPlugins;
    }

    public void setNumberPlugins(int numberPlugins) {
        this.numberPlugins = numberPlugins;
    }

    public int getNumberSourceFiles() {
        return numberSourceFiles;
    }

    public void setNumberSourceFiles(int numberSourceFiles) {
        this.numberSourceFiles = numberSourceFiles;
    }

    public int getNumberTestFiles() {
        return numberTestFiles;
    }

    public void setNumberTestFiles(int numberTestFiles) {
        this.numberTestFiles = numberTestFiles;
    }

    public int getNumberLibrariesFailingModule() {
        return numberLibrariesFailingModule;
    }

    public void setNumberLibrariesFailingModule(int numberLibrariesFailingModule) {
        this.numberLibrariesFailingModule = numberLibrariesFailingModule;
    }

    public int getNumberLines() {
        return numberLines;
    }

    public void setNumberLines(int numberLines) {
        this.numberLines = numberLines;
    }
}

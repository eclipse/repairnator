package fr.inria.spirals.repairnator.process.inspectors.properties;

import fr.inria.spirals.repairnator.process.inspectors.properties.builds.Builds;
import fr.inria.spirals.repairnator.process.inspectors.properties.commits.Commits;
import fr.inria.spirals.repairnator.process.inspectors.properties.patchDiff.PatchDiff;
import fr.inria.spirals.repairnator.process.inspectors.properties.projectMetrics.ProjectMetrics;
import fr.inria.spirals.repairnator.process.inspectors.properties.repository.Repository;
import fr.inria.spirals.repairnator.process.inspectors.properties.reproductionBuggyBuild.ReproductionBuggyBuild;
import fr.inria.spirals.repairnator.process.inspectors.properties.tests.Tests;

public class Properties {

    private String version; // this property is specific for bears.json
    private String type;

    private Repository repository;
    private Builds builds;
    private Commits commits;
    private Tests tests;
    private PatchDiff patchDiff;
    private ProjectMetrics projectMetrics;
    private ReproductionBuggyBuild reproductionBuggyBuild;

    public Properties() {
        this.repository = new Repository();
        this.builds = new Builds();
        this.commits = new Commits();
        this.tests = new Tests();
        this.patchDiff = new PatchDiff();
        this.projectMetrics = new ProjectMetrics();
        this.reproductionBuggyBuild = new ReproductionBuggyBuild();
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Repository getRepository() {
        return repository;
    }

    public Builds getBuilds() {
        return builds;
    }

    public Commits getCommits() {
        return commits;
    }

    public Tests getTests() {
        return tests;
    }

    public PatchDiff getPatchDiff() {
        return patchDiff;
    }

    public ProjectMetrics getProjectMetrics() {
        return projectMetrics;
    }

    public ReproductionBuggyBuild getReproductionBuggyBuild() {
        return reproductionBuggyBuild;
    }
}

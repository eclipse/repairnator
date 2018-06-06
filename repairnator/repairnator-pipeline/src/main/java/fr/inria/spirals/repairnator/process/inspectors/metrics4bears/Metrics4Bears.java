package fr.inria.spirals.repairnator.process.inspectors.metrics4bears;

import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.builds.Builds;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.commits.Commits;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.patchDiff.PatchDiff;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.projectMetrics.ProjectMetrics;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.repository.Repository;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.reproductionBuggyBuild.ReproductionBuggyBuild;
import fr.inria.spirals.repairnator.process.inspectors.metrics4bears.tests.Tests;

public class Metrics4Bears {

    private String version;
    private String type;

    private Repository repository;
    private Builds builds;
    private Commits commits;
    private Tests tests;
    private PatchDiff patchDiff;
    private ProjectMetrics projectMetrics;
    private ReproductionBuggyBuild reproductionBuggyBuild;

    public Metrics4Bears() {
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

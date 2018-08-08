package fr.inria.spirals.repairnator.process.inspectors.properties.commits;

public class Commits {

    private Commit buggyBuild;
    private Commit fixerBuild;
    private Commit buggyBuildForkRepo;
    private Commit buggyBuildBaseRepo;
    private Commit fixerBuildForkRepo;
    private Commit fixerBuildBaseRepo;

    public Commits() {}

    public Commit getBuggyBuild() {
        return buggyBuild;
    }

    public void setBuggyBuild(Commit buggyBuild) {
        this.buggyBuild = buggyBuild;
    }

    public Commit getFixerBuild() {
        return fixerBuild;
    }

    public void setFixerBuild(Commit fixerBuild) {
        this.fixerBuild = fixerBuild;
    }

    public Commit getBuggyBuildForkRepo() {
        return buggyBuildForkRepo;
    }

    public void setBuggyBuildForkRepo(Commit buggyBuildForkRepo) {
        this.buggyBuildForkRepo = buggyBuildForkRepo;
    }

    public Commit getBuggyBuildBaseRepo() {
        return buggyBuildBaseRepo;
    }

    public void setBuggyBuildBaseRepo(Commit buggyBuildBaseRepo) {
        this.buggyBuildBaseRepo = buggyBuildBaseRepo;
    }

    public Commit getFixerBuildForkRepo() {
        return fixerBuildForkRepo;
    }

    public void setFixerBuildForkRepo(Commit fixerBuildForkRepo) {
        this.fixerBuildForkRepo = fixerBuildForkRepo;
    }

    public Commit getFixerBuildBaseRepo() {
        return fixerBuildBaseRepo;
    }

    public void setFixerBuildBaseRepo(Commit fixerBuildBaseRepo) {
        this.fixerBuildBaseRepo = fixerBuildBaseRepo;
    }
}

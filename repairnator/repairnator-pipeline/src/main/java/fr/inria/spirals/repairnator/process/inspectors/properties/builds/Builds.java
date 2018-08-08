package fr.inria.spirals.repairnator.process.inspectors.properties.builds;

public class Builds {

    private Build buggyBuild;
    private Build fixerBuild;

    public Builds() {}

    public Build getBuggyBuild() {
        return buggyBuild;
    }

    public void setBuggyBuild(Build buggyBuild) {
        this.buggyBuild = buggyBuild;
    }

    public Build getFixerBuild() {
        return fixerBuild;
    }

    public void setFixerBuild(Build fixerBuild) {
        this.fixerBuild = fixerBuild;
    }

}

package fr.inria.spirals.repairnator.step;

import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.ProjectInspector;
import fr.inria.spirals.repairnator.ProjectState;
import org.apache.maven.cli.MavenCli;
import org.codehaus.plexus.classworlds.ClassWorld;

/**
 * Created by urli on 03/01/2017.
 */
public class BuildProject extends AbstractStep {

    public BuildProject(ProjectInspector inspector) {
        super(inspector);
    }

    protected int mavenBuild(boolean withTests) {
        final ClassWorld classWorld = new ClassWorld("plexus.core", getClass().getClassLoader());

        MavenCli cli = new MavenCli(classWorld);

        if (!withTests) {
            System.setProperty("maven.test.skip","true");
        }

        int result = cli.doMain(new String[]{"test"},
                this.inspector.getRepoLocalPath(),
                System.out, System.err);

        if (!withTests) {
            System.setProperty("maven.test.skip","false");
        }

        return result;
    }

    public void execute() {
        Launcher.LOGGER.debug("Start building project with maven (skip tests).");
        int result = this.mavenBuild(false);

        if (result == 0) {
            this.state = ProjectState.BUILDABLE;
            this.executeNextStep();
        } else {
            Launcher.LOGGER.info("Repository "+this.inspector.getRepoSlug()+" cannot be built.");
        }
    }
}

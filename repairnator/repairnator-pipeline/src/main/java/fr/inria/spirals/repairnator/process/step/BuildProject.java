package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;

import java.util.Properties;

/**
 * Created by urli on 03/01/2017.
 */
public class BuildProject extends AbstractStep {

    public BuildProject(ProjectInspector inspector) {
        super(inspector);
    }

    public BuildProject(ProjectInspector inspector, String stepName) {
        super(inspector, stepName);
    }

    protected void businessExecute() {
        this.getLogger().debug("Building project with maven (skip tests)...");

        Properties properties = new Properties();
        properties.setProperty(MavenHelper.SKIP_TEST_PROPERTY, "true");

        this.getLogger().debug("Installing artifacts without test execution...");
        MavenHelper helper = new MavenHelper(this.getPom(), "install", properties, this.getClass().getSimpleName(), this.inspector, true);

        int result = helper.run();

        if (result == MavenHelper.MAVEN_SUCCESS) {
            this.setPipelineState(PipelineState.BUILDABLE);
        } else {
            this.getLogger().warn("Repository " + this.inspector.getRepoSlug() + " cannot be built.");
            this.setPipelineState(PipelineState.NOTBUILDABLE);
            this.shouldStop = true;
        }
    }

}

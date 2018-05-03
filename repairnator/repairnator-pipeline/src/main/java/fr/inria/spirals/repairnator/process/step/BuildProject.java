package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;

import java.util.Properties;

/**
 * Created by urli on 03/01/2017.
 */
public class BuildProject extends AbstractStep {

    public BuildProject(ProjectInspector inspector) {
        super(inspector, true);
    }

    public BuildProject(ProjectInspector inspector, String stepName, boolean blockingStep) {
        super(inspector, stepName, blockingStep);
    }

    protected StepStatus businessExecute() {
        this.getLogger().debug("Building project with maven (skip tests)...");

        Properties properties = new Properties();
        properties.setProperty(MavenHelper.SKIP_TEST_PROPERTY, "true");

        this.getLogger().debug("Installing artifacts without test execution...");
        MavenHelper helper = new MavenHelper(this.getPom(), "install", properties, this.getClass().getSimpleName(), this.getInspector(), true);

        int result;
        try {
            result = helper.run();
        } catch (InterruptedException e) {
            this.addStepError("Error while building", e);
            result = MavenHelper.MAVEN_ERROR;
        }

        if (result == MavenHelper.MAVEN_SUCCESS) {
            return StepStatus.buildSuccess();
        } else {
            this.getLogger().warn("Repository " + this.getInspector().getRepoSlug() + " cannot be built.");
            return StepStatus.buildError("Repository " + this.getInspector().getRepoSlug() + " cannot be built.");
        }
    }

}

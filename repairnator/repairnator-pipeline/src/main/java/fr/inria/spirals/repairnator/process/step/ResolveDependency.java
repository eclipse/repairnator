package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;

/**
 * Created by urli on 14/04/2017.
 */
public class ResolveDependency extends AbstractStep {

    public ResolveDependency(ProjectInspector inspector) {
        super(inspector, true);
    }

    public ResolveDependency(ProjectInspector inspector, String stepName, boolean blockingStep) {
        super(inspector, stepName, blockingStep);
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().debug("Resolve dependencies with maven (skip tests)...");

        this.getLogger().debug("Installing artifacts without test execution...");
        MavenHelper helper = new MavenHelper(this.getPom(), "dependency:resolve", null, this.getClass().getSimpleName(), this.getInspector(), true);

        int result = MavenHelper.MAVEN_ERROR;
        try {
            result = helper.run();
        } catch (InterruptedException e) {
            this.addStepError("Error while executing Maven goal", e);
        }

        if (result == MavenHelper.MAVEN_SUCCESS) {
            return StepStatus.buildSuccess(this);
        } else {
            this.getLogger().warn("Repository " + this.getInspector().getRepoSlug() + " may have unresolvable dependencies.");
            return StepStatus.buildError(this,"Repository " + this.getInspector().getRepoSlug() + " have unresolvable dependencies.");
        }
    }


}

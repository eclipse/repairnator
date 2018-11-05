package fr.inria.spirals.repairnator.process.step.paths;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.states.PipelineState;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by bloriot97 on 01/11/2018.
 * This step compute the projects plugins.
 */
public class ComputePlugins extends AbstractStep {

    public ComputePlugins(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
    }

    private List<Plugin> findPlugins(String pomPath) {
        List<File> plugins = new ArrayList<>();

        File pomFile = new File(pomPath);
        Model model = MavenHelper.readPomXml(pomFile, this.getInspector().getM2LocalPath());
        if (model == null) {
            this.addStepError("Error while building model: no model has been retrieved.");
            return null;
        }
        if (model.getBuild() == null) {
            this.addStepError("Error while obtaining build from pom.xml: build section has not been found.");
            return null;
        }
        if (model.getBuild().getPlugins() == null) {
            this.addStepError("Error while obtaining plugins from pom.xml: plugin section has not been found.");
            return null;
        }

        return model.getBuild().getPlugins();
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().debug("Computing project plugins...");

        String mainPomPath = this.getPom();
        List<Plugin> plugins = this.findPlugins(mainPomPath);

        if (plugins == null) {
            this.getLogger().info("No plugins was found.");
            return StepStatus.buildError(this, PipelineState.PLUGINSNOTCOMPUTED);
        } else if (plugins.size() == 0) {
            this.getLogger().info("No plugins was found.");
        }
        this.getInspector().getJobStatus().setPlugins(plugins);
        this.getInspector().getJobStatus().getProperties().getProjectMetrics().setNumberPlugins(plugins.size());
        return StepStatus.buildSuccess(this);
    }
}

package fr.inria.spirals.repairnator.process.step.paths;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

        if (plugins == null || plugins.size() == 0) {
            this.getLogger().info("No plugins was found.");
            // return StepStatus.buildError(this, PipelineState.PLUGINSNOTCOMPUTED);
        }
        this.getInspector().getJobStatus().setPlugins(plugins);
        this.getInspector().getJobStatus().getProperties().getProjectMetrics().setNumberPlugins(plugins.size());
        return StepStatus.buildSuccess(this);
    }
}

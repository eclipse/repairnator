package fr.inria.spirals.repairnator.process.step.paths;

import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.states.PipelineState;
import org.apache.maven.model.Model;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ComputeModules extends AbstractStep {

    public ComputeModules(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
    }

    private File[] findModules(String pomPath, boolean rootCall) {
        List<File> modules = new ArrayList<>();

        File pomFile = new File(pomPath);
        Model model = MavenHelper.readPomXml(pomFile, this.getInspector().getM2LocalPath());
        if (model == null) {
            this.addStepError("Error while building model: no model has been retrieved.");
            return null;
        }
        if (model.getModules() == null) {
            this.addStepError("Error while obtaining modules from pom.xml: module section has not been found.");
            return null;
        }

        for (String moduleName : model.getModules()) {
            File module = new File(pomFile.getParent() + File.separator + moduleName);
            modules.add(module);
            File[] moreModules = this.findModules(module.getPath() + File.separator + Utils.POM_FILE, false);
            if (moreModules != null && moreModules.length > 0) {
                modules.addAll(Arrays.asList(moreModules));
            }
        }

        if (rootCall && modules.size() == 0) {
            modules.add(pomFile.getParentFile());
        }

        return modules.toArray(new File[modules.size()]);
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().debug("Computing project modules...");

        String mainPomPath = this.getPom();
        File[] modules = this.findModules(mainPomPath, true);

        if (modules == null || modules.length == 0) {
            this.getLogger().info("No module was computed.");
            return StepStatus.buildError(this, PipelineState.MODULESNOTCOMPUTED);
        }
        this.getInspector().getJobStatus().setModules(modules);
        this.getInspector().getJobStatus().getProperties().getProjectMetrics().setNumberModules(modules.length);
        return StepStatus.buildSuccess(this);
    }
}

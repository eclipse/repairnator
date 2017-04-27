package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by urli on 08/02/2017.
 */
public class ComputeSourceDir extends AbstractStep {
    private static final String DEFAULT_SRC_DIR = "/src/main/java";

    public ComputeSourceDir(ProjectInspector inspector) {
        super(inspector);
    }

    private File[] searchForSourcesDirectory(String incriminatedModulePath, boolean rootCall) {
        List<File> result = new ArrayList<File>();
        File defaultSourceDir = new File(incriminatedModulePath + DEFAULT_SRC_DIR);

        if (defaultSourceDir.exists()) {
            result.add(defaultSourceDir);
            return result.toArray(new File[result.size()]);
        }

        this.getLogger().debug("The default source directory (" + defaultSourceDir.getPath()
                + ") does not exists. Try to read pom.xml to get informations.");
        File pomIncriminatedModule = new File(incriminatedModulePath + "/pom.xml");

        if (!pomIncriminatedModule.exists()) {
            pomIncriminatedModule = new File(this.getPom());
        }

        try {
            Model model = MavenHelper.readPomXml(pomIncriminatedModule, this.inspector.getM2LocalPath());

            Build buildSection = model.getBuild();

            if (buildSection != null) {
                String pathSrcDirFromPom = model.getBuild().getSourceDirectory();

                File srcDirFromPom = new File(pathSrcDirFromPom);

                if (srcDirFromPom.exists()) {
                    result.add(srcDirFromPom);
                    return result.toArray(new File[result.size()]);
                }

                this.getLogger().debug("The source directory given in pom.xml (" + pathSrcDirFromPom
                        + ") does not exists. Try to get source dir from all modules if multimodule.");
            } else {
                this.getLogger().debug(
                        "Build section does not exists in this pom.xml. Try to get source dir from all modules.");
            }

            for (String module : model.getModules()) {
                File[] srcDir = this.searchForSourcesDirectory(pomIncriminatedModule.getParent() + File.separator + module,
                        false);
                if (srcDir != null) {
                    result.addAll(Arrays.asList(srcDir));
                }
            }

            if (result.size() > 0) {
                return result.toArray(new File[result.size()]);
            }

            if (model.getParent() != null && rootCall) {
                String relativePath = "../pom.xml";

                if (model.getParent().getRelativePath() != null) {
                    relativePath = model.getParent().getRelativePath();
                }

                File parentPomXml = new File(incriminatedModulePath + File.separator + relativePath);

                if (parentPomXml.exists()) {
                    File[] srcDir = this.searchForSourcesDirectory(parentPomXml.getParent(),false);
                    if (srcDir != null) {
                        result.addAll(Arrays.asList(srcDir));
                    }

                    if (result.size() > 0) {
                        return result.toArray(new File[result.size()]);
                    }
                }
            }


        } catch (ModelBuildingException e) {
            this.addStepError("Error while building pom.xml model: " + e);
        }

        this.addStepError(
                "Source directory is not at default location or specified in build section and no parent can be found.");
        return null;
    }

    @Override
    protected void businessExecute() {
        this.getLogger().debug("Computing the source directory ...");
        String incriminatedModule = this.inspector.getJobStatus().getFailingModulePath();

        File[] sources = this.searchForSourcesDirectory(incriminatedModule, true);

        if (sources == null) {
            this.addStepError("Fail to find the sources directory.");
            this.setPipelineState(PipelineState.SOURCEDIRNOTCOMPUTED);
            this.inspector.getJobStatus().setRepairSourceDir(null);
        } else {
            this.inspector.getJobStatus().setRepairSourceDir(sources);
            this.setPipelineState(PipelineState.SOURCEDIRCOMPUTED);
        }
    }

}

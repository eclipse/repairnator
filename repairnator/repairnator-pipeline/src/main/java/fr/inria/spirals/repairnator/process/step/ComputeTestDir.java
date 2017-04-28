package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.states.PipelineState;
import org.apache.commons.io.FileUtils;
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
public class ComputeTestDir extends AbstractStep {
    private static final String DEFAULT_TEST_DIR = "/src/test/java";
    private static final String COMPUTE_TOTAL_CLOC = "cloc --json --vcs=git .";

    public ComputeTestDir(ProjectInspector inspector) {
        super(inspector);
    }

    public ComputeTestDir(ProjectInspector inspector, String name) {
        super(inspector, name);
    }

    private File[] searchForSourcesDirectory(String incriminatedModulePath, boolean rootCall) {
        List<File> result = new ArrayList<File>();
        File defaultTestDir = new File(incriminatedModulePath + DEFAULT_TEST_DIR);

        if (defaultTestDir.exists()) {
            result.add(defaultTestDir);
        }

        this.getLogger().debug("The default test directory (" + defaultTestDir.getPath()
                + ") does not exists. Try to read pom.xml to get informations.");
        File pomIncriminatedModule = new File(incriminatedModulePath + "/pom.xml");

        if (!pomIncriminatedModule.exists()) {
            pomIncriminatedModule = new File(this.getPom());
        }

        try {
            Model model = MavenHelper.readPomXml(pomIncriminatedModule, this.inspector.getM2LocalPath());

            Build buildSection = model.getBuild();

            if (buildSection != null) {
                String pathTestDirFromPom = model.getBuild().getTestSourceDirectory();

                File srcDirFromPom = new File(pathTestDirFromPom);

                if (srcDirFromPom.exists()) {
                    result.add(srcDirFromPom);
                    return result.toArray(new File[result.size()]);
                }

                this.getLogger().debug("The test directory given in pom.xml (" + pathTestDirFromPom
                        + ") does not exists. Try to get test dir from all modules if multimodule.");
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

    private void computeMetricsOnTest(File[] sources) {
        int totalAppFiles = 0;
        if (sources != null && sources.length > 0) {
            for (File f : sources) {
                int nbFile = FileUtils.listFiles(f, new String[] {"java"}, true).size();
                totalAppFiles += nbFile;
            }
            this.inspector.getJobStatus().getMetrics().setNbFileTests(totalAppFiles);
        }
    }

    @Override
    protected void businessExecute() {
        this.getLogger().debug("Computing the test directory ...");
        File[] sources = this.searchForSourcesDirectory(this.inspector.getRepoLocalPath(), true);

        this.computeMetricsOnTest(sources);

        if (sources == null) {
            this.addStepError("Fail to find the sources directory.");
            this.setPipelineState(PipelineState.TESTDIRCOMPUTED);
        } else {
            this.setPipelineState(PipelineState.TESTDIRNOTCOMPUTED);
        }
    }

}

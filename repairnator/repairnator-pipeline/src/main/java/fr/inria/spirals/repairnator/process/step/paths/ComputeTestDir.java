package fr.inria.spirals.repairnator.process.step.paths;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.states.PipelineState;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by urli on 08/02/2017.
 */
public class ComputeTestDir extends AbstractStep {
    private static final String DEFAULT_TEST_DIR = "/src/test/java";
    private static final String COMPUTE_TOTAL_CLOC = "cloc --json --vcs=git .";
    private Set<File> visitedFiles = new HashSet<>();

    public ComputeTestDir(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
    }

    public ComputeTestDir(ProjectInspector inspector, boolean blockingStep, String name) {
        super(inspector, blockingStep, name);
    }

    private File[] searchForSourcesDirectory(String incriminatedModulePath, boolean rootCall) {
        List<File> result = new ArrayList<File>();
        File defaultTestDir = new File(incriminatedModulePath + DEFAULT_TEST_DIR);

        if (defaultTestDir.exists()) {
            result.add(defaultTestDir);
            return result.toArray(new File[0]);
        } else {

            this.getLogger().debug("The default test directory (" + defaultTestDir.getPath()
                    + ") does not exist. Try to read pom.xml to get information.");
            File pomIncriminatedModule = new File(incriminatedModulePath + "/pom.xml");

            if (!pomIncriminatedModule.exists()) {
                pomIncriminatedModule = new File(this.getPom());
            }

            if (this.visitedFiles.contains(pomIncriminatedModule)) {
                this.getLogger().info("It seems we are entering in a loop while searching the test dir. The following file has already been visited: " + pomIncriminatedModule.getAbsolutePath());
                return result.toArray(new File[0]);
            } else {
                this.visitedFiles.add(pomIncriminatedModule);
            }

            try {
                Model model = MavenHelper.readPomXml(pomIncriminatedModule, this.getInspector().getM2LocalPath());

                Build buildSection = model.getBuild();

                if (buildSection != null && buildSection.getTestSourceDirectory() != null) {
                    String pathTestDirFromPom = buildSection.getTestSourceDirectory();

                    File srcDirFromPom = new File(pathTestDirFromPom);

                    if (srcDirFromPom.exists()) {
                        result.add(srcDirFromPom);
                        return result.toArray(new File[result.size()]);
                    }

                    this.getLogger().debug("The test directory given in pom.xml (" + pathTestDirFromPom
                            + ") does not exist. Try to get test dir from all modules if multimodule.");
                } else {
                    this.getLogger().debug(
                            "Build section does not exist in this pom.xml. Try to get source dir from all modules.");
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
                        File[] srcDir = this.searchForSourcesDirectory(parentPomXml.getParent(), false);
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
            return result.toArray(new File[0]);
        }
    }

    private void computeMetricsOnTest(File[] sources) {
        int totalAppFiles = 0;
        if (sources != null && sources.length > 0) {
            for (File f : sources) {
                int nbFile = FileUtils.listFiles(f, new String[] {"java"}, true).size();
                totalAppFiles += nbFile;
            }
            this.getInspector().getJobStatus().getMetrics().setNbFileTests(totalAppFiles);
            this.getInspector().getJobStatus().getMetrics4Bears().getProjectMetrics().setNumberTestFiles(totalAppFiles);
        }
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().debug("Computing the test directory ...");
        File[] sources = this.searchForSourcesDirectory(this.getInspector().getRepoLocalPath(), true);

        if (sources == null || sources.length == 0) {
            this.addStepError("Fail to find the tests directory.");
            return StepStatus.buildError(this, PipelineState.TESTDIRNOTCOMPUTED);
        } else {
            if (sources.length == 1) {
                this.getLogger().info(sources.length+" one test dir was found:");
            } else {
                this.getLogger().info(sources.length+" test dirs were found:");
            }
            for (File file : sources) {
                this.getLogger().info(file.getAbsolutePath());
            }

            this.getInspector().getJobStatus().setTestDir(sources);
            this.computeMetricsOnTest(sources);
            return StepStatus.buildSuccess(this);
        }
    }

}

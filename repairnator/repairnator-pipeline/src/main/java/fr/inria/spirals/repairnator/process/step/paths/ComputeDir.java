package fr.inria.spirals.repairnator.process.step.paths;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;

import java.io.File;
import java.util.*;

public class ComputeDir extends AbstractStep {

    private static final String DEFAULT_SRC_DIR = "/src/main/java";
    private static final String DEFAULT_TEST_DIR = "/src/test/java";

    private ComputeDirType computeDirType;
    private String dirTypeName;
    private String defaultDir;
    private String dirPath;
    private boolean allModules;

    private Set<File> visitedFiles = new HashSet<>();
    private File[] resultDirs;

    public ComputeDir(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
        this.allModules = true;
    }

    private File[] searchForSourcesDirectory(String incriminatedModulePath, boolean rootCall) {
        List<File> result = new ArrayList<File>();
        File defaultSourceDir = new File(incriminatedModulePath + this.defaultDir);

        boolean wasDefaultSourceDirFound = false;
        if (defaultSourceDir.exists()) {
            wasDefaultSourceDirFound = true;
            result.add(defaultSourceDir);
            if (!this.allModules) {
                return result.toArray(new File[result.size()]);
            }
        } else {
            this.getLogger().debug("The default " + dirTypeName + " directory (" + defaultSourceDir.getPath()
                    + ") does not exist. Try to read pom.xml to get information.");
        }
        File pomIncriminatedModule = new File(incriminatedModulePath + "/pom.xml");

        if (!pomIncriminatedModule.exists()) {
            pomIncriminatedModule = new File(this.getPom());
        }

        if (this.visitedFiles.contains(pomIncriminatedModule)) {
            this.getLogger().info("It seems we are entering in a loop while searching the " + dirTypeName + " dir. The following file has already been visited: "+pomIncriminatedModule.getAbsolutePath());
            return result.toArray(new File[0]);
        } else {
            this.visitedFiles.add(pomIncriminatedModule);
        }

        try {
            Model model = MavenHelper.readPomXml(pomIncriminatedModule, this.getInspector().getM2LocalPath());
            if (model == null) {
                this.addStepError("Error while building model: no model has been retrieved.");
                return null;
            }
            if (!wasDefaultSourceDirFound) {
                Build buildSection = model.getBuild();

                if (buildSection != null) {
                    if ((this.computeDirType == ComputeDirType.COMPUTE_SOURCE_DIR &&
                            buildSection.getSourceDirectory() != null) ||
                            this.computeDirType == ComputeDirType.COMPUTE_TEST_DIR &&
                            buildSection.getTestSourceDirectory() != null) {
                        String pathSrcDirFromPom = this.computeDirType == ComputeDirType.COMPUTE_SOURCE_DIR ?
                                buildSection.getSourceDirectory() : buildSection.getTestSourceDirectory();

                        if (pathSrcDirFromPom != null) {
                            File srcDirFromPom = new File(pathSrcDirFromPom);

                            if (srcDirFromPom.exists()) {
                                result.add(srcDirFromPom);
                                return result.toArray(new File[result.size()]);
                            }

                            this.getLogger().debug("The " + dirTypeName + " directory given in pom.xml (" + pathSrcDirFromPom
                                    + ") does not exists. Try to get " + dirTypeName + " dir from all modules if multimodule.");
                        } else {
                            this.getLogger().debug("The " + dirTypeName + " directory has not been found in pom.xml. Try to get " + dirTypeName + " dir from all modules.");
                        }
                    } else {
                        this.getLogger().debug(
                                "Build section does not exist in this pom.xml. Try to get " + dirTypeName + " dir from all modules.");
                    }
                } else {
                    this.getLogger().debug(
                            "Build section does not exist in this pom.xml. Try to get " + dirTypeName + " dir from all modules.");
                }
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
                "The " + dirTypeName + " directory is not at default location or specified in build section and no parent can be found.");
        return null;
    }

    protected int computeMetricsOnDirs(File[] sources) {
        int totalAppFiles = 0;
        if (sources != null && sources.length > 0) {
            for (File f : sources) {
                int nbFile = FileUtils.listFiles(f, new String[] {"java"}, true).size();
                totalAppFiles += nbFile;
            }
        }
        return totalAppFiles;
    }

    @Override
    protected StepStatus businessExecute() {
        this.resultDirs = this.searchForSourcesDirectory(this.dirPath, true);

        if (this.resultDirs == null || this.resultDirs.length == 0) {
            this.addStepError("Fail to find " + dirTypeName + " directories.");
            return StepStatus.buildSkipped(this);
        } else {
            if (this.resultDirs.length == 1) {
                this.getLogger().info(this.resultDirs.length + " " + dirTypeName +" dir was found:");
            } else {
                this.getLogger().info(this.resultDirs.length + " " + dirTypeName +" dirs were found:");
            }
            for (File file : this.resultDirs) {
                this.getLogger().info(file.getAbsolutePath());
            }
            return StepStatus.buildSuccess(this);
        }
    }

    public void setComputeDirType(ComputeDirType computeDirType) {
        this.computeDirType = computeDirType;
        if (this.computeDirType == ComputeDirType.COMPUTE_SOURCE_DIR) {
            this.dirTypeName = "source";
            this.defaultDir = DEFAULT_SRC_DIR;
        } else {
            this.dirTypeName = "test";
            this.defaultDir = DEFAULT_TEST_DIR;
        }
    }

    public void setDirPath(String dirPath) {
        this.dirPath = dirPath;
    }

    public void setAllModules(boolean allModules) {
        this.allModules = allModules;
    }

    public File[] getResultDirs() {
        return resultDirs;
    }
}

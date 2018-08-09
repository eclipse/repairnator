package fr.inria.spirals.repairnator.process.step.paths;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ComputeDir extends AbstractStep {

    private static final String DEFAULT_SRC_DIR = "/src/main/java";
    private static final String DEFAULT_TEST_DIR = "/src/test/java";

    private ComputeDirType computeDirType;
    private String dirTypeName;
    private String defaultDir;
    private String rootDirPath;
    private boolean allModules;

    private Set<File> visitedPomFiles = new HashSet<>();
    private File[] resultDirs;

    public ComputeDir(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
        this.allModules = true;
    }

    protected File[] searchForDirs(String dirPathStr, boolean rootCall) {
        if (dirTypeName == null || defaultDir == null) {
            throw new IllegalStateException("dirTypeName and defaultDir are null. setComputeDirType() should be called first.");
        }

        Set<File> result = new HashSet<File>();

        boolean wasDefaultDirFound = false;

        File dirPath = new File(dirPathStr);
        try {
            dirPath = dirPath.getCanonicalFile();
        } catch (IOException e) {
            getLogger().error("Error while getting canonical path for rootDirectory");
        }

        File defaultDir = new File(dirPath, this.defaultDir);
        if (defaultDir.exists()) {
            wasDefaultDirFound = true;
            result.add(defaultDir);
            if (!this.allModules) {
                return this.returnResult(result);
            }
        } else {
            this.getLogger().debug("The default " + dirTypeName + " directory (" + defaultDir.getPath()
                    + ") does not exist. Try to read pom.xml to get information.");
        }

        File pomOfCurrentDirPath = new File(dirPath + "/pom.xml");
        if (!pomOfCurrentDirPath.exists()) {
            pomOfCurrentDirPath = new File(this.getPom());
        }

        if (this.visitedPomFiles.contains(pomOfCurrentDirPath)) {
            this.getLogger().info("It seems the step is entering in a loop while searching the " + dirTypeName +
                    " dir. The following file has already been visited: "+pomOfCurrentDirPath.getAbsolutePath());
            return result.toArray(new File[0]);
        } else {
            this.visitedPomFiles.add(pomOfCurrentDirPath);
        }

        Model model;
        model = MavenHelper.readPomXml(pomOfCurrentDirPath, this.getInspector().getM2LocalPath());
        if (model == null) {
            this.addStepError("Error while building pom.xml model: no model has been retrieved.");
            return null;
        }

        if (!wasDefaultDirFound) {
            Build buildSection = model.getBuild();
            if (buildSection != null) {
                if ((this.computeDirType == ComputeDirType.COMPUTE_SOURCE_DIR &&
                        buildSection.getSourceDirectory() != null) ||
                        this.computeDirType == ComputeDirType.COMPUTE_TEST_DIR &&
                        buildSection.getTestSourceDirectory() != null) {

                    String pathDirFromPom = this.computeDirType == ComputeDirType.COMPUTE_SOURCE_DIR ?
                            buildSection.getSourceDirectory() : buildSection.getTestSourceDirectory();

                    if (pathDirFromPom != null) {
                        File dirFromPom = new File(pathDirFromPom);

                        if (dirFromPom.exists()) {
                            result.add(dirFromPom);
                            return this.returnResult(result);
                        }

                        this.getLogger().debug("The " + dirTypeName + " directory given in pom.xml (" + pathDirFromPom
                                + ") does not exist. Continue to try to get " + dirTypeName + " dir from all modules.");
                    } else {
                        this.getLogger().debug("The " + dirTypeName + " directory has not been found in the following pom.xml: "
                                + pomOfCurrentDirPath + ". Continue to try to get " + dirTypeName + " dir from all modules.");
                    }
                } else {
                    this.getLogger().debug("Build section does not exist in the following pom.xml: "
                            + pomOfCurrentDirPath + ". Continue to try to get " + dirTypeName + " dir from all modules.");
                }
            } else {
                this.getLogger().debug("Build section does not exist in the following pom.xml: "
                        + pomOfCurrentDirPath + ". Continue to try to get " + dirTypeName + " dir from all modules.");
            }
        }

        if (model.getModules() != null) {
            for (String module : model.getModules()) {
                File[] dirs = this.searchForDirs(pomOfCurrentDirPath.getParent() + File.separator + module, false);
                if (dirs != null) {
                    result.addAll(Arrays.asList(dirs));
                }
            }
        } else {
            this.getLogger().debug("No module has been found in the following pom.xml: " + pomOfCurrentDirPath + ".");
        }

        if (model.getParent() != null && rootCall) {
            String relativePath = "../pom.xml";

            if (model.getParent().getRelativePath() != null) {
                relativePath = model.getParent().getRelativePath();
            }

            File parentPomXml = new File(dirPath + File.separator + relativePath);

            try {
                parentPomXml = parentPomXml.getCanonicalFile();
            } catch (IOException e) {
                getLogger().error("Error while getting canonical path for parentPomXml");
            }

            if (parentPomXml.exists()) {
                File[] dirs = this.searchForDirs(parentPomXml.getParent(),false);
                if (dirs != null) {
                    result.addAll(Arrays.asList(dirs));
                }

                if (result.size() > 0) {
                    return this.returnResult(result);
                }
            }
        }

        if (result.size() > 0) {
            return this.returnResult(result);
        }

        this.addStepError("The " + dirTypeName +
                " directory is not at default location or specified in build section from pom.xml, and no parent can be found.");
        return null;
    }

    private File[] returnResult(Set<File> result) {
        List<File> sortedFiles = new ArrayList<>(result);
        Collections.sort(sortedFiles);
        return sortedFiles.toArray(new File[sortedFiles.size()]);
    }

    protected int computeMetricsOnDirs(File[] dirs) {
        int numberFiles = 0;
        if (dirs != null && dirs.length > 0) {
            for (File f : dirs) {
                numberFiles += FileUtils.listFiles(f, new String[] {"java"}, true).size();
            }
        }
        return numberFiles;
    }

    @Override
    protected StepStatus businessExecute() {
        this.resultDirs = this.searchForDirs(this.rootDirPath, true);

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

    public void setRootDirPath(String rootDirPath) {
        this.rootDirPath = rootDirPath;
    }

    public void setAllModules(boolean allModules) {
        this.allModules = allModules;
    }

    public File[] getResultDirs() {
        return resultDirs;
    }
}

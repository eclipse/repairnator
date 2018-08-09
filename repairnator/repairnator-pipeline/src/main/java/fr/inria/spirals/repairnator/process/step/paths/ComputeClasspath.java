package fr.inria.spirals.repairnator.process.step.paths;

import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.step.StepStatus;
import fr.inria.spirals.repairnator.process.step.AbstractStep;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Created by urli on 08/02/2017.
 */
public class ComputeClasspath extends AbstractStep {
    private static final String goal = "dependency:build-classpath";

    private static final String CLASSPATH_FILENAME = "classpath.info";
    private static final String DEFAULT_CLASSES_DIR = "/target/classes";
    private static final String DEFAULT_TEST_CLASSES_DIR = "/target/test-classes";

    private List<URL> classPath;

    public ComputeClasspath(ProjectInspector inspector, boolean blockingStep) {
        super(inspector, blockingStep);
        this.classPath = new ArrayList<>();
    }

    private void addFileToClassPath(File file) {
        if (file.exists()) {
            try {
                this.classPath.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                this.addStepError("Error while adding the following file in the classpath: " +
                        file.getAbsolutePath() + ".", e);
            }
        } else {
            this.addStepError("The file does not exist: " + file.getAbsolutePath() + ".");
        }
    }

    private void addDefaultDirsToClassPath(String modulePath) {
        File defaultClassDir = new File(modulePath + File.separator + DEFAULT_CLASSES_DIR + File.separator);
        this.addFileToClassPath(defaultClassDir);

        File defaultTestClassDir = new File(
                modulePath + File.separator + DEFAULT_TEST_CLASSES_DIR + File.separator);
        this.addFileToClassPath(defaultTestClassDir);
    }

    private int runMavenGoal(String pomPath, Properties properties) {
        MavenHelper helper = new MavenHelper(pomPath, goal, properties, this.getClass().getSimpleName(),
                this.getInspector(), true);

        int result = MavenHelper.MAVEN_ERROR;
        try {
            result = helper.run();
        } catch (InterruptedException e) {
            this.addStepError("Error while executing maven goal.", e);
        }
        return result;
    }

    private void addJarFilesToClassPath(String classpathFilePath) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(classpathFilePath)));
            String classpathLine = reader.readLine();

            if (classpathLine != null && !classpathLine.isEmpty()) {
                String[] allJars = classpathLine.split(":");

                for (String jar : allJars) {
                    File jarFile = new File(jar);
                    this.addFileToClassPath(jarFile);
                }
                this.getInspector().getJobStatus().addFileToPush(CLASSPATH_FILENAME);
            } else {
                this.addStepError("The classpath file is empty.");
            }
        } catch (IOException e) {
            this.addStepError("Problem while getting classpath file.", e);
        }
    }

    private void checkJUnitInClasspath() {
        boolean containJunit = false;
        for (URL url : this.classPath) {
            if (url.toString().contains("junit")) {
                containJunit = true;
            }
        }
        if (!containJunit) {
            this.addStepError("The classpath seems not to contain JUnit, maybe this project does not use JUnit for testing.");
        }
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().debug("Computing classpath from incriminated module...");

        String incriminatedModule = this.getInspector().getJobStatus().getFailingModulePath();

        Properties properties = new Properties();
        properties.setProperty("mdep.outputFile", CLASSPATH_FILENAME);

        String pomModule = incriminatedModule + File.separator + Utils.POM_FILE;
        String classpathFilePath = incriminatedModule + File.separator + CLASSPATH_FILENAME;

        if (this.runMavenGoal(pomModule, properties) != MavenHelper.MAVEN_SUCCESS) {
            this.addStepError("Error while computing classpath maven.");
            return StepStatus.buildError(this, PipelineState.CLASSPATHERROR);
        }

        // Only jars will be added in the classpath here, which is the number of libraries of the failing module
        this.addJarFilesToClassPath(classpathFilePath);
        this.getInspector().getJobStatus().getProperties().getProjectMetrics().setNumberLibrariesFailingModule(this.classPath.size());

        this.checkJUnitInClasspath();

        // Default "/target/classes" and "/target/test-classes" dirs are then added here
        this.addDefaultDirsToClassPath(incriminatedModule);
        this.getInspector().getJobStatus().setRepairClassPath(this.classPath);

        return StepStatus.buildSuccess(this);
    }
}

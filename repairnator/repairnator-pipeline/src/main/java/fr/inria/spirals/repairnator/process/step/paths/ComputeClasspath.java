package fr.inria.spirals.repairnator.process.step.paths;

import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.process.inspectors.StepStatus;
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
                this.addStepError(e.getMessage());
                this.getLogger().error("Error while putting the following file in classpath: " + file.getAbsolutePath()
                        + " : " + e.getMessage(), e);
            }
        } else {
            this.addStepError("The file does not exist: " + file.getAbsolutePath());
        }
    }

    @Override
    protected StepStatus businessExecute() {
        this.getLogger().debug("Computing classpath from incriminated module...");

        String incriminatedModule = this.getInspector().getJobStatus().getFailingModulePath();

        File defaultClassDir = new File(incriminatedModule + File.separator + DEFAULT_CLASSES_DIR + File.separator);
        this.addFileToClassPath(defaultClassDir);

        File defaultTestClassDir = new File(
                incriminatedModule + File.separator + DEFAULT_TEST_CLASSES_DIR + File.separator);
        this.addFileToClassPath(defaultTestClassDir);

        Properties properties = new Properties();
        properties.setProperty("mdep.outputFile", CLASSPATH_FILENAME);

        String goal = "dependency:build-classpath";
        String pomModule = incriminatedModule + File.separator + Utils.POM_FILE;

        MavenHelper helper = new MavenHelper(pomModule, goal, properties, this.getClass().getSimpleName(),
                this.getInspector(), true);

        int result = MavenHelper.MAVEN_ERROR;
        try {
            result = helper.run();
        } catch (InterruptedException e) {
            this.addStepError("Error with maven goal", e);
        }

        if (result != MavenHelper.MAVEN_SUCCESS) {
            this.getLogger().debug("Error while computing classpath maven");
            return StepStatus.buildError(this, PipelineState.CLASSPATHERROR);
        }

        String classpathPath = incriminatedModule + File.separator + CLASSPATH_FILENAME;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(classpathPath)));
            String classpathLine = reader.readLine();

            String[] allJars = classpathLine.split(":");

            for (String jar : allJars) {
                File f = new File(jar);
                this.addFileToClassPath(f);
            }
            this.getInspector().getJobStatus().addFileToPush(CLASSPATH_FILENAME);
        } catch (IOException e) {
            this.addStepError("Problem while getting classpath: " + e);
        }

        boolean containJunit = false;
        for (URL url : classPath) {
            if (url.toString().contains("junit")) {
                containJunit = true;
                break;
            }
        }

        if (!containJunit) {
            this.addStepError("The classpath seems not to contain JUnit, maybe this project does not use junit for testing.");
        }

        this.getInspector().getJobStatus().setRepairClassPath(this.classPath);
        this.getInspector().getJobStatus().getMetrics().setNbLibraries(this.classPath.size() - 2);
        this.getInspector().getJobStatus().getMetrics4Bears().getProjectMetrics().setNumberLibraries(this.classPath.size() - 2);

        return StepStatus.buildSuccess(this);
    }
}

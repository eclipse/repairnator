package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.ProjectState;
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

    public ComputeClasspath(ProjectInspector inspector) {
        super(inspector);
        this.classPath = new ArrayList<>();
    }

    private void addFileToClassPath(File file) {
        if (file.exists()) {
            try {
                this.classPath.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                this.addStepError(e.getMessage());
                this.getLogger().error("Error while putting the following file in classpath: "+file.getAbsolutePath()+" : "+e.getMessage(),e);
            }
        } else {
            this.addStepError("The file does not exist: "+file.getAbsolutePath());
        }
    }

    @Override
    protected void businessExecute() {
        this.getLogger().debug("Compute classpath from incriminated module...");

        GatherTestInformation infoStep = inspector.getTestInformations();
        String incriminatedModule = infoStep.getFailingModulePath();

        File defaultClassDir = new File(incriminatedModule+File.separator+DEFAULT_CLASSES_DIR+File.separator);
        this.addFileToClassPath(defaultClassDir);

        File defaultTestClassDir = new File(incriminatedModule+File.separator+DEFAULT_TEST_CLASSES_DIR+File.separator);
        this.addFileToClassPath(defaultTestClassDir);

        Properties properties = new Properties();
        properties.setProperty("mdep.outputFile",CLASSPATH_FILENAME);

        String goal = "dependency:build-classpath";
        String pomModule = incriminatedModule+ File.separator+"pom.xml";

        MavenHelper helper = new MavenHelper(pomModule, goal, properties, this.getClass().getSimpleName(), this.inspector, true);

        int result = helper.run();

        if (result != MavenHelper.MAVEN_SUCCESS) {
            this.getLogger().debug("Error while computing classpath maven");
            this.shouldStop = true;
            return;
        }

        String classpathPath = incriminatedModule+File.separator+CLASSPATH_FILENAME;

        try {
            BufferedReader reader = new BufferedReader(new FileReader(new File(classpathPath)));
            String classpathLine = reader.readLine();

            String[] allJars = classpathLine.split(":");
            for (String jar : allJars) {
                File f = new File(jar);
                this.addFileToClassPath(f);
            }
        } catch (IOException e) {
            this.addStepError("Problem while getting classpath: "+e);
            this.shouldStop = true;
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

        this.inspector.setRepairClassPath(this.classPath);
        this.setState(ProjectState.CLASSPATHCOMPUTED);
    }
}

package fr.inria.spirals.repairnator.process.maven;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.output.MavenErrorHandler;
import fr.inria.spirals.repairnator.process.maven.output.MavenFilterOutputHandler;
import fr.inria.spirals.repairnator.process.maven.output.MavenMuteOutputHandler;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilder;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * This class intends to help the usage of maven goals in Repairnator
 */
public class MavenHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenHelper.class);
    public static final int MAVEN_SUCCESS = 0;
    public static final int MAVEN_ERROR = 1;

    public static final String SKIP_TEST_PROPERTY = "maven.test.skip.exec";

    // all the goals we want to skip
    // fixme: make that list available in a config
    private static final List<String> SKIP_LIST = Arrays.asList(
            "enforcer.skip",
            "checkstyle.skip",
            "cobertura.skip",
            "skipITs",
            "rat.skip",
            "license.skip",
            "findbugs.skip",
            "gpg.skip",
            "skip.npm",
            "skip.gulp",
            "skip.bower",
            "dependency-check.skip"
    );
    private static final int TIMEOUT_WITHOUT_OUTPUT = 10; // in minutes

    private final Logger logger = LoggerFactory.getLogger(MavenHelper.class);

    private String goal;
    private String pomFile;
    private Properties properties;
    private String name;
    private ProjectInspector inspector;
    private Instant limitOutputDate;

    private InvocationOutputHandler errorHandler;
    private InvocationOutputHandler outputHandler;

    public MavenHelper(String pomFile, String goal, Properties properties, String name, ProjectInspector inspector, boolean enableHandlers) {
        this.goal = goal;
        this.pomFile = pomFile;
        this.properties = properties;
        this.name = name;
        this.inspector = inspector;

        if (enableHandlers) {
            this.errorHandler = new MavenErrorHandler(this);
            this.outputHandler = new MavenFilterOutputHandler(this);
        } else {
            this.outputHandler = new MavenMuteOutputHandler(this);
        }

        this.updateProperties();
    }

    private void updateProperties() {
        if (this.properties == null) {
            this.properties = new Properties();
        }

        // we want to use a dedicated Maven repository
        this.properties.setProperty("maven.repo.local", this.inspector.getM2LocalPath());
        for (String skip : SKIP_LIST) {
            this.properties.setProperty(skip, "true");
        }
    }

    public String getGoal() {
        return goal;
    }

    public String getPomFile() {
        return pomFile;
    }

    public Properties getProperties() {
        return properties;
    }

    public String getName() {
        return name;
    }

    public ProjectInspector getInspector() {
        return inspector;
    }

    public InvocationOutputHandler getErrorHandler() {
        return errorHandler;
    }

    public InvocationOutputHandler getOutputHandler() {
        return outputHandler;
    }

    public void updateLastOutputDate() {
        this.limitOutputDate = new Date().toInstant().plus(TIMEOUT_WITHOUT_OUTPUT, ChronoUnit.MINUTES);
    }

    public void setErrorHandler(InvocationOutputHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void setOutputHandler(InvocationOutputHandler outputHandler) {
        this.outputHandler = outputHandler;
    }

    public static Model readPomXml(File pomXml, String localMavenRepository) {
        ModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setProcessPlugins(true);
        req.setPomFile(pomXml);
        req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        req.setModelResolver(new RepositoryModelResolver(localMavenRepository));

        DefaultModelBuilder defaultModelBuilder = new DefaultModelBuilderFactory().newInstance();

        // we try to build the model, and if we fail, we try to get the raw model
        try {
            ModelBuildingResult modelBuildingResult = defaultModelBuilder.build(req);
            return modelBuildingResult.getEffectiveModel();
        } catch (ModelBuildingException e) {
            LOGGER.error("Error while building complete model. The raw model will be used. Error message: "+e.getMessage());
            return defaultModelBuilder.buildRawModel(pomXml, ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL, true).get();
        }

    }

    // we manage our own timeout
    public int run() throws InterruptedException {
        RunnableMavenInvoker runnableMavenInvoker = new RunnableMavenInvoker(this);
        Thread t = new Thread(runnableMavenInvoker);
        this.updateLastOutputDate();
        t.start();

        while (t.isAlive()) {
            Instant now = new Date().toInstant();

            if (now.isAfter(this.limitOutputDate)) {
                t.interrupt();
                throw new InterruptedException("Timeout occurred: no output has been received in the last "+TIMEOUT_WITHOUT_OUTPUT+" minutes.");
            } else {
                Thread.sleep(1000);
            }
        }

        return runnableMavenInvoker.getExitCode();
    }
}

package fr.inria.spirals.repairnator.process.maven;

import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.output.MavenErrorHandler;
import fr.inria.spirals.repairnator.process.maven.output.MavenFilterOutputHandler;
import fr.inria.spirals.repairnator.process.maven.output.MavenMuteOutputHandler;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

/**
 * Created by urli on 10/01/2017.
 */
public class MavenHelper {
    public static final int MAVEN_SUCCESS = 0;
    public static final int MAVEN_ERROR = 1;

    public static final String SKIP_TEST_PROPERTY = "maven.test.skip.exec";

    private final Logger logger = LoggerFactory.getLogger(MavenHelper.class);

    private String goal;
    private String pomFile;
    private Properties properties;
    private String name;
    private ProjectInspector inspector;

    private InvocationOutputHandler errorHandler;
    private InvocationOutputHandler outputHandler;

    public MavenHelper(String pomFile, String goal, Properties properties, String name, ProjectInspector inspector, boolean enableHandlers) {
        this.goal = goal;
        this.pomFile = pomFile;
        this.properties = properties;
        this.name = name;
        this.inspector = inspector;

        if (enableHandlers) {
            this.errorHandler = new MavenErrorHandler(this.inspector, this.name);
            this.outputHandler = new MavenFilterOutputHandler(this.inspector, this.name);
        } else {
            this.outputHandler = new MavenMuteOutputHandler(this.inspector, this.name);
        }
    }

    public void setErrorHandler(InvocationOutputHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public void setOutputHandler(InvocationOutputHandler outputHandler) {
        this.outputHandler = outputHandler;
    }

    public static Model readPomXml(File pomXml, String localMavenRepository) throws ModelBuildingException {
        ModelBuildingRequest req = new DefaultModelBuildingRequest();
        req.setProcessPlugins(false);
        req.setPomFile(pomXml);
        req.setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL);
        req.setModelResolver(new RepositoryModelResolver(localMavenRepository));

        return new DefaultModelBuilderFactory().newInstance().build(req).getEffectiveModel();
    }

    public int run() {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(this.pomFile));
        request.setGoals(Arrays.asList(this.goal));

        if (properties == null) {
            properties = new Properties();
        }

        properties.setProperty("maven.repo.local", this.inspector.getM2LocalPath());
        properties.setProperty("enforcer.skip", "true");
        properties.setProperty("checkstyle.skip", "true");
        properties.setProperty("cobertura.skip", "true");
        properties.setProperty("skipITs", "true");
        properties.setProperty("rat.skip", "true");
        properties.setProperty("license.skip", "true");
        properties.setProperty("findbugs.skip", "true");
        properties.setProperty("gpg.skip", "true");
        request.setProperties(properties);

        Invoker invoker = new DefaultInvoker();

        if (this.errorHandler != null) {
            invoker.setErrorHandler(this.errorHandler);
        }
        invoker.setOutputHandler(this.outputHandler);

        try {
            InvocationResult result = invoker.execute(request);
            return result.getExitCode();
        } catch (MavenInvocationException e) {
            this.logger.error("Error while executing goal :" + this.goal + " on the following pom file: " + this.pomFile
                    + ". Properties: " + this.properties);
            this.logger.debug(e.getMessage());
            this.inspector.getJobStatus().addStepError(name, e.getMessage());
            return MAVEN_ERROR;
        }
    }
}

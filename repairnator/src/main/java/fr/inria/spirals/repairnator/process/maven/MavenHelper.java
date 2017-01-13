package fr.inria.spirals.repairnator.process.maven;

import fr.inria.spirals.repairnator.process.ProjectInspector;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
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

    public static final String CLEAN_ARTIFACT_GOAL = "build-helper:remove-project-artifact";
    public static final String CLEAN_DEPENDENCIES_GOAL = "dependency:purge-local-repository";

    private final Logger logger = LoggerFactory.getLogger(MavenHelper.class);

    private String goal;
    private String pomFile;
    private Properties properties;
    private String name;
    private ProjectInspector inspector;
    private boolean enableHandlers;

    public MavenHelper(String pomFile, String goal, Properties properties, String name, ProjectInspector inspector, boolean enableHandlers) {
        this.goal = goal;
        this.pomFile = pomFile;
        this.properties = properties;
        this.name = name;
        this.inspector = inspector;
        this.enableHandlers = enableHandlers;
    }

    public int run() {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile( new File( this.pomFile ) );
        request.setGoals( Arrays.asList( this.goal ) );

        if (properties == null) {
            properties = new Properties();
        }

        properties.setProperty("enforcer.skip","true");
        request.setProperties(properties);

        Invoker invoker = new DefaultInvoker();
        if (this.enableHandlers) {
            invoker.setErrorHandler(new MavenErrorHandler(this.inspector, this.name));
            invoker.setOutputHandler(new MavenFilterOutputHandler(this.inspector, this.name));
        } else {
            invoker.setOutputHandler(new MavenMuteOutputHandler());
        }

        try {
            InvocationResult result = invoker.execute( request );
            return result.getExitCode();
        } catch (MavenInvocationException e) {
            this.logger.error("Error while executing goal :"+this.goal+" on the following pom file: "+this.pomFile+". Properties: "+this.properties);
            this.logger.debug(e.getMessage());
            this.inspector.addStepError(name, e.getMessage());
            return MAVEN_ERROR;
        }
    }
}

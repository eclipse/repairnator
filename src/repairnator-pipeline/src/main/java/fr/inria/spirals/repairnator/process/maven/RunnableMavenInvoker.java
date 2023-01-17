package fr.inria.spirals.repairnator.process.maven;

import fr.inria.spirals.repairnator.config.RepairnatorConfig;
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
 * This class allows us to run a Maven goal in a dedicated thread that we can interrupt for timeout
 */
public class RunnableMavenInvoker implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(RunnableMavenInvoker.class);

    private MavenHelper mavenHelper;
    private int exitCode;

    public RunnableMavenInvoker(MavenHelper mavenHelper) {
        this.mavenHelper = mavenHelper;
        this.exitCode = -1;
    }

    @Override
    public void run() {
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(this.mavenHelper.getPomFile()));
        request.setGoals(Arrays.asList(this.mavenHelper.getGoal()));
        Properties props = this.mavenHelper.getProperties();
        String mavenHome="";
        if (System.getenv("M2_HOME") == null) {
            // sensible value
            // https://stackoverflow.com/questions/14793015/programmatically-launch-m2e-maven-command
             mavenHome = RepairnatorConfig.getInstance().getMavenHome();
            System.out.println("M2_HOME not found, using provided input value instead - " + mavenHome);

            System.setProperty("maven.home", mavenHome);
        } else if ( System.getProperty("maven.home") == null ) {
            mavenHome = System.getenv("M2_HOME");
            System.setProperty("maven.home", mavenHome);
        }
        request.setProperties(props);
        request.setBatchMode(true);
        request.setShowErrors(true);

        Invoker invoker = new DefaultInvoker();

        if (this.mavenHelper.getErrorHandler() != null) {
            invoker.setErrorHandler(this.mavenHelper.getErrorHandler());
        }
        invoker.setOutputHandler(this.mavenHelper.getOutputHandler());
        invoker.setMavenHome(new File(mavenHome));


        try {
            InvocationResult result = invoker.execute(request);
            this.exitCode = result.getExitCode();
        } catch (MavenInvocationException e) {
            this.logger.error("Error while executing goal " + this.mavenHelper.getGoal()
                    + " on the following pom file: " + this.mavenHelper.getPomFile()
                    + ". Properties: " + this.mavenHelper.getProperties());
            this.logger.error(e.getMessage());
            this.mavenHelper.getInspector().getJobStatus().addStepError(this.mavenHelper.getName(), e.getMessage());
            this.exitCode = MavenHelper.MAVEN_ERROR;
        }
    }

    public int getExitCode() {
        return exitCode;
    }
}

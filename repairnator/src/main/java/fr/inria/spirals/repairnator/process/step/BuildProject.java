package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

/**
 * Created by urli on 03/01/2017.
 */
public class BuildProject extends AbstractStep {

    public BuildProject(ProjectInspector inspector) {
        super(inspector);
    }

    protected int mavenBuild(boolean withTests) {
        Properties properties = new Properties();
        if (!withTests) {
            properties.setProperty("maven.test.skip","true");
        }

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile( new File( this.getPom() ) );
        request.setGoals( Arrays.asList( "test" ) );
        request.setProperties(properties);

        Invoker invoker = new DefaultInvoker();

        if (!withTests) {
            MavenErrorHandler errorHandler = new MavenErrorHandler(this.inspector, this.getClass().getName());
            invoker.setErrorHandler(errorHandler);
        }

        try {
            InvocationResult result = invoker.execute( request );

            return result.getExitCode();
        } catch (MavenInvocationException e) {
            this.getLogger().debug("Error while launching tests goal :"+e);
            this.addStepError(e.getMessage());
            return 1;
        }

    }

    protected void businessExecute() {
        this.getLogger().debug("Start building project with maven (skip tests).");

        int result = this.mavenBuild(false);

        if (result == 0) {
            this.state = ProjectState.BUILDABLE;
        } else {
            this.getLogger().info("Repository "+this.inspector.getRepoSlug()+" cannot be built.");
            this.shouldStop = true;
        }
    }
}

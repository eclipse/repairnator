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

/**
 * Created by urli on 03/01/2017.
 */
public class BuildProject extends AbstractStep {

    public BuildProject(ProjectInspector inspector) {
        super(inspector);
    }

    protected int mavenBuild(boolean withTests) {
        if (!withTests) {
            System.setProperty("maven.test.skip","true");
        }

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile( new File( this.getPom() ) );
        request.setGoals( Arrays.asList( "test" ) );

        Invoker invoker = new DefaultInvoker();
        try {
            InvocationResult result = invoker.execute( request );

            if (!withTests) {
                System.clearProperty("maven.test.skip");
            }

            if (result.getExitCode() != 0) {
                this.addStepError(result.getExecutionException().getMessage());
            }
            return result.getExitCode();
        } catch (MavenInvocationException e) {
            Launcher.LOGGER.debug("Error while launching tests goal :"+e);
            this.addStepError(e.getMessage());
            return 1;
        }

    }

    protected void businessExecute() {
        Launcher.LOGGER.debug("Start building project with maven (skip tests).");

        int result = this.mavenBuild(false);

        if (result == 0) {
            this.state = ProjectState.BUILDABLE;
        } else {
            Launcher.LOGGER.info("Repository "+this.inspector.getRepoSlug()+" cannot be built.");
            this.shouldStop = true;
        }
    }
}

package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import org.codehaus.plexus.util.cli.CommandLineException;

import java.io.File;
import java.util.Date;
import java.util.List;

/**
 * Created by urli on 03/01/2017.
 */
public abstract class AbstractStep {

    private String name;
    private int limitStepNumber;
    protected ProjectInspector inspector;
    protected ProjectState state;

    protected boolean shouldStop;
    private AbstractStep nextStep;
    private long dateBegin;
    private long dateEnd;

    public AbstractStep(ProjectInspector inspector) {
        this.name = this.getClass().getName();
        this.inspector = inspector;
        this.shouldStop = false;
        this.state = ProjectState.NONE;
    }

    public void setLimitStepNumber(int limitStepNumber) {
        this.limitStepNumber = limitStepNumber;
    }

    public AbstractStep setNextStep(AbstractStep nextStep) {
        this.nextStep = nextStep;
        return nextStep;
    }

    public ProjectState getState() {
        return state;
    }

    public void setState(ProjectState state) {
        this.state = state;
    }

    protected void addStepError(String error) {
        this.inspector.addStepError(this.name, error);
    }

    protected ProjectState executeNextStep() {
        if (this.nextStep != null) {
            this.limitStepNumber--;
            Launcher.LOGGER.debug(this.limitStepNumber+" steps remaining...");

            if (this.limitStepNumber > 0) {
                this.nextStep.setLimitStepNumber(this.limitStepNumber);
                this.nextStep.setState(this.state);
                return this.nextStep.execute();
            }

        }
        return this.state;
    }

    protected String getPom() {
        return this.inspector.getRepoLocalPath()+File.separator+"pom.xml";
    }

    /*protected MavenCli getMavenCli() {
        final ClassWorld classWorld = new ClassWorld("plexus.core", getClass().getClassLoader());
        System.setProperty("maven.multiModuleProjectDirectory",this.inspector.getRepoLocalPath());
        MavenCli cli = new MavenCli(classWorld);
        return cli;
    }*/

    public ProjectState execute() {
        this.dateBegin = new Date().getTime();
        this.businessExecute();
        this.dateEnd = new Date().getTime();
        if (!shouldStop) {
            return this.executeNextStep();
        } else {
            return this.state;
        }
    }

    public int getDuration() {
        if (dateEnd == 0 || dateBegin == 0) {
            return 0;
        }
        return Math.round((dateEnd-dateBegin) / 1000);
    }

    protected abstract void businessExecute();
}

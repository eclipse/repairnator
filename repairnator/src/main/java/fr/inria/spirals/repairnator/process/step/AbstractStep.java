package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Properties;

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

    protected Logger getLogger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    protected void addStepError(String error) {
        this.inspector.addStepError(this.name, error);
    }

    protected ProjectState executeNextStep() {
        if (this.nextStep != null) {
            this.limitStepNumber--;
            this.getLogger().debug(this.limitStepNumber+" steps remaining...");

            if (this.limitStepNumber > 0) {
                this.nextStep.setLimitStepNumber(this.limitStepNumber);
                this.nextStep.setState(this.state);
                return this.nextStep.execute();
            }

        }
        this.cleanMavenArtifacts();
        return this.state;
    }

    protected String getPom() {
        return this.inspector.getRepoLocalPath()+File.separator+"pom.xml";
    }

    protected void cleanMavenArtifacts() {
        MavenHelper helper = new MavenHelper(this.getPom(), MavenHelper.CLEAN_ARTIFACT_GOAL, null, this.getClass().getName(), this.inspector, true);
        helper.run();

        Properties properties = new Properties();
        properties.setProperty(MavenHelper.CLEAN_DEPENDENCIES_PROPERTY,"false");
        helper = new MavenHelper(this.getPom(), MavenHelper.CLEAN_DEPENDENCIES_GOAL, properties, this.getClass().getName(), this.inspector, true);
        helper.run();
    }

    public ProjectState execute() {
        this.dateBegin = new Date().getTime();
        this.businessExecute();
        this.dateEnd = new Date().getTime();
        if (!shouldStop) {
            return this.executeNextStep();
        } else {
            this.cleanMavenArtifacts();
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

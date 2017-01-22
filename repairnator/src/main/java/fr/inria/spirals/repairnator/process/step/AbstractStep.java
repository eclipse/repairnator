package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import fr.inria.spirals.repairnator.process.maven.MavenHelper;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
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
    private boolean pomLocationTested;
    private List<AbstractDataSerializer> serializers;

    public AbstractStep(ProjectInspector inspector) {
        this.name = this.getClass().getName();
        this.inspector = inspector;
        this.shouldStop = false;
        this.state = ProjectState.NONE;
        this.pomLocationTested = false;
        this.serializers = new ArrayList<AbstractDataSerializer>();
    }

    public void setDataSerializer(List<AbstractDataSerializer> serializers) {
        this.serializers = serializers;
    }

    public void setLimitStepNumber(int limitStepNumber) {
        this.limitStepNumber = limitStepNumber;
    }

    public AbstractStep setNextStep(AbstractStep nextStep) {
        this.nextStep = nextStep;
        nextStep.setDataSerializer(this.serializers);
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
        getLogger().error(error);
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
        this.serializeData();
        return this.state;
    }

    private void serializeData() {
        for (AbstractDataSerializer serializer : this.serializers) {
            serializer.serializeData(this.inspector);
        }
    }

    private void testPomLocation() {
        this.pomLocationTested = true;
        File defaultPomFile = new File(this.inspector.getRepoLocalPath()+File.separator+"pom.xml");

        if (defaultPomFile.exists()) {
            return;
        } else {
            this.getLogger().info("The pom.xml file is not at the root of the repository. Try to find another one.");

            File rootRepo = new File(this.inspector.getRepoLocalPath());

            File[] dirs = rootRepo.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
            });

            for (File dir : dirs) {
                File pomFile = new File(dir.getPath()+File.separator+"pom.xml");

                if (pomFile.exists()) {
                    this.getLogger().info("Found a pom.xml in the following directory: "+dir.getPath());
                    this.inspector.setRepoLocalPath(dir.getPath());
                    return;
                }
            }

            this.addStepError("RepairNator was unable to found a pom.xml in the repository. It will stop now.");
            this.shouldStop = true;
        }
    }

    protected String getPom() {
        if (!pomLocationTested) {
            testPomLocation();
        }
        return this.inspector.getRepoLocalPath()+File.separator+"pom.xml";
    }

    protected void cleanMavenArtifacts() {
        try {
            FileUtils.deleteDirectory(this.inspector.getM2LocalPath());
        } catch (IOException e) {
            getLogger().warn("Error while deleting the M2 local directory ("+this.inspector.getM2LocalPath()+"): "+e);
        }

        if (this.inspector.isAutoclean()) {
            try {
                FileUtils.deleteDirectory(this.inspector.getRepoLocalPath());
            } catch (IOException e) {
                getLogger().warn("Error while deleting the workspace directory ("+this.inspector.getRepoLocalPath()+"): "+e);
            }
        }
    }

    public ProjectState execute() {
        this.dateBegin = new Date().getTime();
        this.businessExecute();
        this.dateEnd = new Date().getTime();
        if (!shouldStop) {
            return this.executeNextStep();
        } else {
            this.cleanMavenArtifacts();
            this.serializeData();
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

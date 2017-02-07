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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Created by urli on 03/01/2017.
 */
public abstract class AbstractStep {
    public static final String PROPERTY_FILENAME = "repairnator.properties";
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
    private Properties properties;

    public AbstractStep(ProjectInspector inspector) {
        this.name = this.getClass().getName();
        this.inspector = inspector;
        this.shouldStop = false;
        this.state = ProjectState.NONE;
        this.pomLocationTested = false;
        this.serializers = new ArrayList<AbstractDataSerializer>();
        this.properties = new Properties();
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
        if (this.nextStep != null) {
            this.nextStep.setProperties(properties);
        }
    }

    public String getName() {
        return name;
    }

    public void setDataSerializer(List<AbstractDataSerializer> serializers) {
        if (serializers != null) {
            this.serializers = serializers;
            if (this.nextStep != null) {
                this.nextStep.setDataSerializer(serializers);
            }
        }
    }

    public void setLimitStepNumber(int limitStepNumber) {
        this.limitStepNumber = limitStepNumber;
    }

    public AbstractStep setNextStep(AbstractStep nextStep) {
        this.nextStep = nextStep;
        nextStep.setDataSerializer(this.serializers);
        nextStep.setProperties(this.properties);
        nextStep.setLimitStepNumber(this.limitStepNumber);
        nextStep.setState(this.state);
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

    protected void executeNextStep() {
        if (this.nextStep != null) {
            this.limitStepNumber--;
            this.getLogger().debug(this.limitStepNumber+" steps remaining...");

            if (this.limitStepNumber > 0) {
                this.nextStep.setLimitStepNumber(this.limitStepNumber);
                this.nextStep.setState(this.state);
                this.nextStep.execute();
                return;
            }
        } else {
            this.inspector.setState(this.state);
            this.serializeData();
            this.cleanMavenArtifacts();
        }
    }

    private void serializeData() {
        if (serializers != null) {
            for (AbstractDataSerializer serializer : this.serializers) {
                serializer.serializeData(this.inspector);
            }
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
        this.writeProperty("lastStep",this.getName());
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

    public void execute() {
        this.dateBegin = new Date().getTime();
        this.businessExecute();
        this.dateEnd = new Date().getTime();

        this.inspector.addStepDuration(this.name, getDuration());

        if (!shouldStop) {
            this.executeNextStep();
        } else {
            this.cleanMavenArtifacts();
            this.inspector.setState(this.state);
            this.serializeData();
        }
    }

    private int getDuration() {
        if (dateEnd == 0 || dateBegin == 0) {
            return 0;
        }
        return Math.round((dateEnd-dateBegin) / 1000);
    }

    protected void writeProperty(String property, String value) {
        this.properties.setProperty(property, value);

        String filePath = this.inspector.getRepoLocalPath()+File.separator+PROPERTY_FILENAME;
        File file = new File(filePath);

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            OutputStream outputStream = new FileOutputStream(file);

            this.properties.store(outputStream, "Repairnator properties");
        } catch (IOException e) {
            this.getLogger().error("Cannot write property to the following file: "+filePath, e);
        }
    }

    protected abstract void businessExecute();
}
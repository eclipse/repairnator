package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.ProjectState;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Created by urli on 03/01/2017.
 */
public abstract class AbstractStep {
    public static final String PROPERTY_FILENAME = "repairnator.properties";
    private String name;
    protected ProjectInspector inspector;
    protected ProjectState state;

    protected boolean shouldStop;
    private AbstractStep nextStep;
    private long dateBegin;
    private long dateEnd;
    private boolean pomLocationTested;
    private List<AbstractDataSerializer> serializers;
    private Properties properties;
    private RepairnatorConfig config;

    public AbstractStep(ProjectInspector inspector) {
        this(inspector, "");
        this.name = this.getClass().getSimpleName();
    }

    public AbstractStep(ProjectInspector inspector, String name) {
        this.name = name;
        this.inspector = inspector;
        this.shouldStop = false;
        this.setState(ProjectState.NONE);
        this.pomLocationTested = false;
        this.serializers = new ArrayList<AbstractDataSerializer>();
        this.properties = new Properties();
        this.config = RepairnatorConfig.getInstance();
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
        if (this.nextStep != null) {
            this.nextStep.setProperties(properties);
        }
    }

    protected Properties getProperties() {
        return properties;
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

    public ProjectInspector getInspector() {
        return inspector;
    }

    public AbstractStep setNextStep(AbstractStep nextStep) {
        this.nextStep = nextStep;
        nextStep.setDataSerializer(this.serializers);
        nextStep.setProperties(this.properties);
        nextStep.setState(this.state);
        return nextStep;
    }

    public ProjectState getState() {
        return state;
    }

    public void setState(ProjectState state) {
        if (state != null) {
            this.state = state;
            if (this.nextStep != null) {
                this.nextStep.setState(state);
            }
        }
    }

    protected Logger getLogger() {
        return LoggerFactory.getLogger(this.getClass());
    }

    public void addStepError(String error) {
        getLogger().error(error);
        this.inspector.getJobStatus().addStepError(this.name, error);
    }

    public void addStepError(String error, Throwable exception) {
        getLogger().error(error, exception);
        this.inspector.getJobStatus().addStepError(this.name, error);
    }

    protected void executeNextStep() {
        if (this.nextStep != null) {
            this.nextStep.setState(this.state);
            this.nextStep.execute();
        } else {
            this.terminatePipeline();
        }
    }

    private void serializeData() {
        if (serializers != null) {
            this.getLogger().info("Serialize all data for build: "+this.getInspector().getBuild().getId());
            for (AbstractDataSerializer serializer : this.serializers) {
                serializer.serializeData(this.inspector);
            }
        }
    }

    private void testPomLocation() {
        this.pomLocationTested = true;
        File defaultPomFile = new File(this.inspector.getRepoLocalPath() + File.separator + "pom.xml");

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

            if (dirs != null) {
                Arrays.sort(dirs);
                for (File dir : dirs) {
                    File pomFile = new File(dir.getPath()+File.separator+"pom.xml");

                    if (pomFile.exists()) {
                        this.getLogger().info("Found a pom.xml in the following directory: "+dir.getPath());
                        this.inspector.getJobStatus().setPomDirPath(dir.getPath());
                        return;
                    }
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
        return this.inspector.getRepoLocalPath() + File.separator + "pom.xml";
    }

    protected void cleanMavenArtifacts() {
        if (this.inspector.getM2LocalPath() != null) {
            try {
                FileUtils.deleteDirectory(this.inspector.getM2LocalPath());
            } catch (IOException e) {
                getLogger().warn(
                        "Error while deleting the M2 local directory (" + this.inspector.getM2LocalPath() + "): " + e);
            }
        }

        if (this.config.isClean()) {
            try {
                FileUtils.deleteDirectory(this.inspector.getRepoLocalPath());
            } catch (IOException e) {
                getLogger().warn("Error while deleting the workspace directory (" + this.inspector.getRepoLocalPath()
                        + "): " + e);
            }
        }
    }

    public void execute() {
        this.dateBegin = new Date().getTime();
        this.businessExecute();
        this.dateEnd = new Date().getTime();

        this.inspector.getJobStatus().addStepDuration(this.name, getDuration());
        this.pushNewInformationIfNeeded();

        if (!shouldStop) {
            this.executeNextStep();
        } else {
            this.terminatePipeline();
        }
    }

    private void terminatePipeline() {
        this.writeProperty("lastStep", this.getName());
        this.inspector.getJobStatus().setState(this.state);
        this.serializeData();
        this.pushNewInformationIfNeeded();
        this.cleanMavenArtifacts();
    }

    private void pushNewInformationIfNeeded() {
        try {
            Git git = Git.open(new File(this.inspector.getRepoLocalPath()));

            this.writeProperty("step-durations", StringUtils.join(this.inspector.getJobStatus().getStepsDurationsInSeconds().entrySet()));

            boolean createNewCommit = this.getInspector().getGitHelper().addAndCommitRepairnatorLogAndProperties(git, "Commit done at the end of step "+this.getName());

            if (createNewCommit && this.inspector.getJobStatus().isHasBeenPushed()) {
                CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(System.getenv("GITHUB_OAUTH"), "");

                git.push().setRemote(PushIncriminatedBuild.REMOTE_NAME).setCredentialsProvider(credentialsProvider).call();
            }
        } catch (IOException | GitAPIException  e) {
            this.getLogger().error("Error while committing (and/or pushing) new repairnator information. ", e);
        }

    }

    private int getDuration() {
        if (dateEnd == 0 || dateBegin == 0) {
            return 0;
        }
        return Math.round((dateEnd - dateBegin) / 1000);
    }

    protected void writeProperty(String property, String value) {
        this.properties.setProperty(property, value);

        String filePath = this.inspector.getRepoLocalPath() + File.separator + PROPERTY_FILENAME;
        File file = new File(filePath);

        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            OutputStream outputStream = new FileOutputStream(file);

            this.properties.store(outputStream, "Repairnator properties");
        } catch (IOException e) {
            this.getLogger().error("Cannot write property to the following file: " + filePath, e);
        }
    }

    public RepairnatorConfig getConfig() {
        return config;
    }

    protected abstract void businessExecute();
}

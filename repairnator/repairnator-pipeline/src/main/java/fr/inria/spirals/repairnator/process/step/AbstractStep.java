package fr.inria.spirals.repairnator.process.step;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.inria.spirals.repairnator.process.inspectors.Metrics;
import fr.inria.spirals.repairnator.process.inspectors.MetricsSerializerAdapter;
import fr.inria.spirals.repairnator.process.step.push.PushIncriminatedBuild;
import fr.inria.spirals.repairnator.states.PipelineState;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.notifier.AbstractNotifier;
import fr.inria.spirals.repairnator.process.inspectors.ProjectInspector;
import fr.inria.spirals.repairnator.serializer.AbstractDataSerializer;
import fr.inria.spirals.repairnator.states.PushState;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Created by urli on 03/01/2017.
 */
public abstract class AbstractStep {
    private static final String PROPERTY_FILENAME = "repairnator.json";
    private String name;
    protected ProjectInspector inspector;
    private PipelineState pipelineState;
    private PushState pushState;

    protected boolean shouldStop;
    private AbstractStep nextStep;
    private long dateBegin;
    private long dateEnd;
    private boolean pomLocationTested;
    private List<AbstractDataSerializer> serializers;
    private List<AbstractNotifier> notifiers;
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
        this.setPipelineState(PipelineState.NONE);
        this.setPushState(PushState.NONE);
        this.pomLocationTested = false;
        this.serializers = new ArrayList<AbstractDataSerializer>();
        this.properties = new Properties();
        this.config = RepairnatorConfig.getInstance();
    }

    public void setNotifiers(List<AbstractNotifier> notifiers) {
        if (notifiers != null) {
            this.notifiers = notifiers;
            if (this.nextStep != null) {
                this.nextStep.setNotifiers(notifiers);
            }
        }
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
        nextStep.setNotifiers(this.notifiers);
        nextStep.setProperties(this.properties);
        nextStep.setPipelineState(this.pipelineState);
        return nextStep;
    }

    public PipelineState getPipelineState() {
        return pipelineState;
    }

    public void setPipelineState(PipelineState pipelineState) {
        if (pipelineState != null) {
            this.pipelineState = pipelineState;
            this.inspector.getJobStatus().setPipelineState(this.pipelineState);
            if (this.nextStep != null) {
                this.nextStep.setPipelineState(pipelineState);
            }
        }
    }

    public PushState getPushState() {
        return pushState;
    }

    public void setPushState(PushState pushState) {
        if (pushState != null) {
            this.pushState = pushState;
            this.inspector.getJobStatus().setPushState(this.pushState);
            if (this.nextStep != null) {
                this.nextStep.setPushState(pushState);
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
        this.observeAndNotify();
        if (this.nextStep != null) {
            this.nextStep.setPipelineState(this.pipelineState);
            this.nextStep.execute();
        } else {
            this.terminatePipeline();
        }
    }

    private void serializeData() {
        if (serializers != null) {
            this.getLogger().info("Serialize all data for build: "+this.getInspector().getBuggyBuild().getId());
            for (AbstractDataSerializer serializer : this.serializers) {
                serializer.serializeData(this.inspector);
            }
        }
    }

    private void observeAndNotify() {
        if (this.notifiers != null) {
            for (AbstractNotifier notifier : this.notifiers) {
                notifier.observe(this.inspector);
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

    public boolean isShouldStop() {
        return shouldStop;
    }

    protected String getPom() {
        if (!pomLocationTested) {
            testPomLocation();
        }
        return this.inspector.getJobStatus().getPomDirPath() + File.separator + "pom.xml";
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

        Metrics metric = this.inspector.getJobStatus().getMetrics();
        metric.addStepDuration(this.name, getDuration());
        metric.addFreeMemoryByStep(this.name, Runtime.getRuntime().freeMemory());

        this.inspector.getJobStatus().setPipelineState(this.pipelineState);

        if (!shouldStop) {
            this.executeNextStep();
        } else {
            this.terminatePipeline();
        }
    }

    private void terminatePipeline() {
        this.recordMetrics();
        this.writeProperty("metrics", this.inspector.getJobStatus().getMetrics());
        this.lastPush();
        this.serializeData();
        this.cleanMavenArtifacts();
    }

    private void recordMetrics() {
        Metrics metric = this.inspector.getJobStatus().getMetrics();

        metric.setFreeMemory(Runtime.getRuntime().freeMemory());
        metric.setTotalMemory(Runtime.getRuntime().totalMemory());
        metric.setNbCPU(Runtime.getRuntime().availableProcessors());
    }

    private int getDuration() {
        if (dateEnd == 0 || dateBegin == 0) {
            return 0;
        }
        return Math.round((dateEnd - dateBegin) / 1000);
    }

    private void lastPush() {
        if (RepairnatorConfig.getInstance().isPush() && this.getInspector().getJobStatus().getPushState() != PushState.NONE) {
            File sourceDir = new File(this.getInspector().getRepoLocalPath());
            File targetDir = new File(this.getInspector().getRepoToPushLocalPath());

            try {
                Git git = Git.open(targetDir);

                org.apache.commons.io.FileUtils.copyDirectory(sourceDir, targetDir, new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return !pathname.toString().contains(".git") && !pathname.toString().contains(".m2");
                    }
                });

                git.add().addFilepattern(".").call();
                PersonIdent personIdent = new PersonIdent("Luc Esape", "luc.esape@gmail.com");
                git.commit().setMessage("End of the repairnator process")
                        .setAuthor(personIdent).setCommitter(personIdent).call();

                if (this.getInspector().getJobStatus().isHasBeenPushed()) {
                    CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(this.config.getGithubToken(), "");
                    git.push().setRemote(PushIncriminatedBuild.REMOTE_NAME).setCredentialsProvider(credentialsProvider).call();
                }
            } catch (GitAPIException | IOException e) {
                this.getLogger().error("Error while trying to commit last information for repairnator", e);
            }
        }


    }

    protected void writeProperty(String propertyName, Object value) {
        if (value != null) {
            this.properties.put(propertyName, value);

            String filePath = this.inspector.getRepoLocalPath() + File.separator + PROPERTY_FILENAME;
            File file = new File(filePath);

            Gson gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Metrics.class, new MetricsSerializerAdapter()).create();
            String jsonString = gson.toJson(this.properties);

            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
                OutputStreamWriter outputStream = new OutputStreamWriter(new FileOutputStream(file));
                outputStream.write(jsonString);
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                this.getLogger().error("Cannot write property to the following file: " + filePath, e);
            }
        } else {
            this.getLogger().warn("Trying to write property null for key: "+propertyName);
        }

    }

    public RepairnatorConfig getConfig() {
        return config;
    }

    protected abstract void businessExecute();
}

package fr.inria.spirals.repairnator.dockerpool;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerExit;
import com.spotify.docker.client.messages.HostConfig;
import fr.inria.spirals.repairnator.InputBuildId;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.dockerpool.serializer.TreatedBuildTracking;

import fr.inria.spirals.repairnator.states.LauncherMode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * This class allows the creation of a docker container and run it.
 */
public class RunnablePipelineContainer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunnablePipelineContainer.class);
    private static final int DELAY_BEFORE_KILLING_DOCKER_IMAGE = 60 * 24; // in minutes
    private Date limitDateBeforeKilling;
    private String imageId;
    private InputBuildId inputBuildId;
    private String logDirectory;
    private RepairnatorConfig repairnatorConfig;
    private TreatedBuildTracking treatedBuildTracking;
    private AbstractPoolManager poolManager;
    private String containerId;
    private String containerName;
    private List<String> envValues;
    private Set<String> volumes;

    /**
     * The constructor will init all the environment values for the container.
     *
     * @param poolManager the manager of the pool of running containers
     * @param imageId the name of the docker image to use for creating the container
     * @param inputBuildId the input build to use for specifying the arguments
     * @param logDirectory the path of the produced logs
     * @param treatedBuildTracking a serializer for recording the docker containers statuses
     */
    public RunnablePipelineContainer(AbstractPoolManager poolManager, String imageId, InputBuildId inputBuildId, String logDirectory, TreatedBuildTracking treatedBuildTracking) {
        this.poolManager = poolManager;
        this.imageId = imageId;
        this.inputBuildId = inputBuildId;
        this.logDirectory = logDirectory;
        this.repairnatorConfig = RepairnatorConfig.getInstance();
        this.treatedBuildTracking = treatedBuildTracking;

        this.containerName = "repairnator-pipeline_"+ Utils.formatFilenameDate(new Date())+"_"+this.inputBuildId.getBuggyBuildId()+"_"+this.repairnatorConfig.getRunId();
        String output = (this.repairnatorConfig.isCreateOutputDir()) ? "/var/log/"+this.repairnatorConfig.getRunId() : "/var/log";

        this.envValues = new ArrayList<>();

        // depending on the mode (BEARS or repairnator)
        // we give different arguments
        this.envValues.add("BUILD_ID="+this.inputBuildId.getBuggyBuildId());
        if (this.repairnatorConfig.getLauncherMode() == LauncherMode.BEARS) {
            this.envValues.add("NEXT_BUILD_ID="+this.inputBuildId.getPatchedBuildId());
            if (this.repairnatorConfig.isDebug()) {
                this.envValues.add("LOG_LEVEL=DEBUG");
            } else {
                this.envValues.add("LOG_LEVEL=INFO");
            }
        }
        this.envValues.add("LOG_FILENAME="+this.containerName);
        this.envValues.add("GITHUB_OAUTH="+RepairnatorConfig.getInstance().getGithubToken());
        this.envValues.add("RUN_ID="+this.repairnatorConfig.getRunId());
        this.envValues.add("REPAIR_MODE="+this.repairnatorConfig.getLauncherMode().name().toLowerCase());
        this.envValues.add("PUSH_URL="+this.repairnatorConfig.getPushRemoteRepo());
        this.envValues.add("MONGODB_HOST="+this.repairnatorConfig.getMongodbHost());
        this.envValues.add("MONGODB_NAME="+this.repairnatorConfig.getMongodbName());
        this.envValues.add("SMTP_SERVER="+this.repairnatorConfig.getSmtpServer());
        this.envValues.add("GITHUB_USERNAME="+this.repairnatorConfig.getGithubUserName());
        this.envValues.add("GITHUB_USEREMAIL="+this.repairnatorConfig.getGithubUserEmail());
        this.envValues.add("NOTIFY_TO="+ StringUtils.join(this.repairnatorConfig.getNotifyTo(),','));
        this.envValues.add("OUTPUT="+output);
        if (this.repairnatorConfig.isCreatePR()) {
            this.envValues.add("CREATE_PR=1");
        }

        if (this.repairnatorConfig.getLauncherMode() == LauncherMode.REPAIR) {
            this.envValues.add("REPAIR_TOOLS=" + StringUtils.join(this.repairnatorConfig.getRepairTools(), ","));
        }
    }

    public InputBuildId getInputBuildId() {
        return this.inputBuildId;
    }

    public Date getLimitDateBeforeKilling() {
        return this.limitDateBeforeKilling;
    }

    @Override
    public void run() {
        this.limitDateBeforeKilling = new Date(new Date().toInstant().plus(DELAY_BEFORE_KILLING_DOCKER_IMAGE, ChronoUnit.MINUTES).toEpochMilli());
        DockerClient docker = this.poolManager.getDockerClient();
        try {
            LOGGER.info("Start to build and run container for build id "+this.inputBuildId.getBuggyBuildId());
            LOGGER.info("At most this docker run will be killed at: "+this.limitDateBeforeKilling);

            // fixme: this does not work anymore to put a name that is displayed in docker ps
            Map<String,String> labels = new HashMap<>();
            labels.put("name",this.containerName);

            // inside the docker containers the log will be produced in /var/log
            // so we need to bind the directory with the local directory for logs
            HostConfig hostConfig = HostConfig.builder().appendBinds(this.logDirectory+":/var/log").build();

            // we soecify the complete configuration of the container
            ContainerConfig containerConfig = ContainerConfig.builder()
                    .image(imageId)
                    .env(envValues)
                    .hostname(Utils.getHostname())
                    .hostConfig(hostConfig)
                    .labels(labels)
                    .build();

            // and we create it
            LOGGER.info("(BUILD ID " + this.inputBuildId.getBuggyBuildId() + ") Create the container: "+this.containerName);
            ContainerCreation container = docker.createContainer(containerConfig);

            // fixme: replace it with volumes() ?
            // at the end we want to remove both the container and the volume to save space
            this.volumes = containerConfig.volumeNames();

            this.containerId = container.id();
            treatedBuildTracking.setContainerId(this.containerId);

            // now the container is created: let's start it
            LOGGER.info("(BUILD ID " + this.inputBuildId.getBuggyBuildId() + ") Start the container: "+this.containerName);
            docker.startContainer(container.id());

            // and now we wait until it's finished
            ContainerExit exitStatus = docker.waitContainer(this.containerId);

            LOGGER.info("(BUILD ID " + this.inputBuildId.getBuggyBuildId() + ") The container has finished with status code: "+exitStatus.statusCode());

            // standard bash: if it's 0 everything's fine.
            if (!this.repairnatorConfig.isSkipDelete() && exitStatus.statusCode() == 0) {
                LOGGER.info("(BUILD ID " + this.inputBuildId.getBuggyBuildId() + ") Container will be removed.");
                docker.removeContainer(this.containerId);
                this.removeVolumes(docker);
            }

            if (exitStatus.statusCode() == 0) {
                serialize("TREATED");
            } else {
                serialize("ERROR:CODE" + exitStatus.statusCode());
            }
        } catch (InterruptedException e) {
            LOGGER.error("Error while running the container for build id "+this.inputBuildId.getBuggyBuildId(), e);
            killDockerContainer(docker, false);
        } catch (DockerException e) {
            LOGGER.error("Error while creating or running the container for build id "+this.inputBuildId.getBuggyBuildId(), e);
            serialize("ERROR");
        }
        this.poolManager.removeSubmittedRunnablePipelineContainer(this);
    }

    private void removeVolumes(DockerClient docker) throws DockerException, InterruptedException {
        for (String volume : this.volumes) {
            docker.removeVolume(volume);
        }

    }

    /**
     * In case of timeout, we kill the container.
     * @param remove if true, it will remove both the container and the volumes
     */
    public void killDockerContainer(DockerClient docker, boolean remove) {
        if (this.containerId == null) {
            LOGGER.error("Error while trying to kill docker container: the container id is not available. Maybe the container is not started yet.");
        } else {
            LOGGER.info("Killing the docker container with id "+containerId+". Forced killing date: "+this.limitDateBeforeKilling);
            try {
                docker.killContainer(containerId);
                if (remove) {
                    docker.removeContainer(containerId);
                    this.removeVolumes(docker);
                }
                this.poolManager.removeSubmittedRunnablePipelineContainer(this);
                serialize("INTERRUPTED");
            } catch (DockerException|InterruptedException e) {
                LOGGER.error("Error while killing docker container "+containerId, e);
            }
        }

    }

    public void serialize(String msg) {
        treatedBuildTracking.setStatus(msg);
        treatedBuildTracking.serialize();
    }

}

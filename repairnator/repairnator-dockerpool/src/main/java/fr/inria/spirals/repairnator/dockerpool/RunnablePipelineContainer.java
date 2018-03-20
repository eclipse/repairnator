package fr.inria.spirals.repairnator.dockerpool;

import com.spotify.docker.client.DockerClient;
import com.spotify.docker.client.exceptions.DockerException;
import com.spotify.docker.client.messages.ContainerConfig;
import com.spotify.docker.client.messages.ContainerCreation;
import com.spotify.docker.client.messages.ContainerExit;
import com.spotify.docker.client.messages.HostConfig;
import fr.inria.spirals.repairnator.Utils;
import fr.inria.spirals.repairnator.config.RepairnatorConfig;
import fr.inria.spirals.repairnator.dockerpool.serializer.TreatedBuildTracking;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Created by urli on 14/03/2017.
 */
public class RunnablePipelineContainer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunnablePipelineContainer.class);
    private static final int DELAY_BEFORE_KILLING_DOCKER_IMAGE = 60 * 24; // in minutes
    private Date limitDateBeforeKilling;
    private String imageId;
    private int buildId;
    private int nextBuildId;
    private String logDirectory;
    private RepairnatorConfig repairnatorConfig;
    private TreatedBuildTracking treatedBuildTracking;
    private boolean skipDelete;
    private boolean createOutputDir;
    private AbstractPoolManager poolManager;
    private String containerId;
    String containerName;
    String output;
    private List<String> envValues;
    private Set<String> volumes;

    public RunnablePipelineContainer(AbstractPoolManager poolManager, String imageId, int buildId, String logDirectory, TreatedBuildTracking treatedBuildTracking, boolean skipDelete, boolean createOutputDir) {
        this.poolManager = poolManager;
        this.imageId = imageId;
        this.buildId = buildId;
        this.logDirectory = logDirectory;
        this.repairnatorConfig = RepairnatorConfig.getInstance();
        this.treatedBuildTracking = treatedBuildTracking;
        this.skipDelete = skipDelete;
        this.createOutputDir = createOutputDir;

        this.containerName = "repairnator-pipeline_"+ Utils.formatFilenameDate(new Date())+"_"+this.buildId;
        this.output = (createOutputDir) ? "/var/log/"+this.repairnatorConfig.getRunId() : "/var/log";

        this.envValues = new ArrayList<>();
        this.envValues.add("BUILD_ID="+this.buildId);
        this.envValues.add("LOG_FILENAME="+containerName);
        this.envValues.add("GITHUB_LOGIN="+System.getenv("GITHUB_LOGIN"));
        this.envValues.add("GITHUB_OAUTH="+System.getenv("GITHUB_OAUTH"));
        this.envValues.add("RUN_ID="+this.repairnatorConfig.getRunId());
        this.envValues.add("GOOGLE_ACCESS_TOKEN="+this.repairnatorConfig.getGoogleAccessToken());
        this.envValues.add("REPAIR_MODE="+this.repairnatorConfig.getLauncherMode().name().toLowerCase());
        this.envValues.add("SPREADSHEET_ID="+this.repairnatorConfig.getSpreadsheetId());
        this.envValues.add("PUSH_URL="+this.repairnatorConfig.getPushRemoteRepo());
        this.envValues.add("MONGODB_HOST="+this.repairnatorConfig.getMongodbHost());
        this.envValues.add("MONGODB_NAME="+this.repairnatorConfig.getMongodbName());
        this.envValues.add("SMTP_SERVER="+this.repairnatorConfig.getSmtpServer());
        this.envValues.add("NOTIFY_TO="+ StringUtils.join(this.repairnatorConfig.getNotifyTo(),','));
        this.envValues.add("OUTPUT="+output);
    }

    public RunnablePipelineContainer(AbstractPoolManager poolManager, String imageId, int buildId, int nextBuildId, String logDirectory, TreatedBuildTracking treatedBuildTracking, boolean skipDelete, boolean createOutputDir) {
        this(poolManager, imageId, buildId, logDirectory, treatedBuildTracking, skipDelete, createOutputDir);
        this.nextBuildId = nextBuildId;
        this.envValues.add("NEXT_BUILD_ID="+this.nextBuildId);
    }

    public int getBuildId() {
        return buildId;
    }

    public Date getLimitDateBeforeKilling() {
        return this.limitDateBeforeKilling;
    }

    @Override
    public void run() {
        this.limitDateBeforeKilling = new Date(new Date().toInstant().plus(DELAY_BEFORE_KILLING_DOCKER_IMAGE, ChronoUnit.MINUTES).toEpochMilli());
        DockerClient docker = this.poolManager.getDockerClient();
        try {
            LOGGER.info("Start to build and run container for build id "+buildId);
            LOGGER.info("At most this docker run will be killed at: "+this.limitDateBeforeKilling);



            Map<String,String> labels = new HashMap<>();
            labels.put("name",containerName);
            HostConfig hostConfig = HostConfig.builder().appendBinds(this.logDirectory+":/var/log").build();
            ContainerConfig containerConfig = ContainerConfig.builder()
                    .image(imageId)
                    .env(envValues)
                    .hostname(Utils.getHostname())
                    .hostConfig(hostConfig)
                    .labels(labels)
                    .build();

            LOGGER.info("Create the container: "+containerName);
            ContainerCreation container = docker.createContainer(containerConfig);

            this.volumes = containerConfig.volumeNames();

            this.containerId = container.id();
            treatedBuildTracking.setContainerId(this.containerId);

            LOGGER.info("Start the container: "+containerName);
            docker.startContainer(container.id());

            ContainerExit exitStatus = docker.waitContainer(this.containerId);

            LOGGER.info("The container has finished with status code: "+exitStatus.statusCode());

            if (!skipDelete && exitStatus.statusCode() == 0) {
                LOGGER.info("Container will be removed.");
                docker.removeContainer(this.containerId);
                this.removeVolumes(docker);
            }

            serialize("TREATED");
        } catch (InterruptedException e) {
            LOGGER.error("Error while running the container for build id "+buildId, e);
            killDockerContainer(docker, false);
        } catch (DockerException e) {
            LOGGER.error("Error while creating or running the container for build id "+buildId, e);
            serialize("ERROR");
        }
        this.poolManager.removeSubmittedRunnablePipelineContainer(this);
    }

    private void removeVolumes(DockerClient docker) throws DockerException, InterruptedException {
        for (String volume : this.volumes) {
            docker.removeVolume(volume);
        }

    }

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

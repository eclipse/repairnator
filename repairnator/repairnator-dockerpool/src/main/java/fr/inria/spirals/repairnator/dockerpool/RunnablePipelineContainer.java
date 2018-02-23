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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by urli on 14/03/2017.
 */
public class RunnablePipelineContainer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunnablePipelineContainer.class);
    private static final int DELAY_BEFORE_KILLING_DOCKER_IMAGE = 60 * 24; // in minutes
    private Date limitDateBeforeKilling;
    private String imageId;
    private int buildId;
    private String logDirectory;
    private RepairnatorConfig repairnatorConfig;
    private TreatedBuildTracking treatedBuildTracking;
    private boolean skipDelete;
    private boolean createOutputDir;
    private AbstractPoolManager poolManager;
    private String containerId;

    public RunnablePipelineContainer(AbstractPoolManager poolManager, String imageId, int buildId, String logDirectory, TreatedBuildTracking treatedBuildTracking, boolean skipDelete, boolean createOutputDir) {
        this.poolManager = poolManager;
        this.imageId = imageId;
        this.buildId = buildId;
        this.logDirectory = logDirectory;
        this.repairnatorConfig = RepairnatorConfig.getInstance();
        this.treatedBuildTracking = treatedBuildTracking;
        this.skipDelete = skipDelete;
        this.createOutputDir = createOutputDir;
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
        String containerId = null;
        DockerClient docker = this.poolManager.getDockerClient();
        try {
            LOGGER.info("Start to build and run container for build id "+buildId);
            LOGGER.info("At most this docker run will be killed at: "+this.limitDateBeforeKilling);

            String containerName = "repairnator-pipeline_"+ Utils.formatFilenameDate(new Date())+"_"+this.buildId;
            String output = (createOutputDir) ? "/var/log/"+this.repairnatorConfig.getRunId() : "/var/log";

            String[] envValues = new String[] {
                "BUILD_ID="+this.buildId,
                "LOG_FILENAME="+containerName,
                "GITHUB_LOGIN="+System.getenv("GITHUB_LOGIN"),
                "GITHUB_OAUTH="+System.getenv("GITHUB_OAUTH"),
                "RUN_ID="+this.repairnatorConfig.getRunId(),
                "GOOGLE_ACCESS_TOKEN="+this.repairnatorConfig.getGoogleAccessToken(),
                "REPAIR_MODE="+this.repairnatorConfig.getLauncherMode().name().toLowerCase(),
                "SPREADSHEET_ID="+this.repairnatorConfig.getSpreadsheetId(),
                "PUSH_URL="+this.repairnatorConfig.getPushRemoteRepo(),
                "MONGODB_HOST="+this.repairnatorConfig.getMongodbHost(),
                "MONGODB_NAME="+this.repairnatorConfig.getMongodbName(),
                "SMTP_SERVER="+this.repairnatorConfig.getSmtpServer(),
                "NOTIFY_TO="+ StringUtils.join(this.repairnatorConfig.getNotifyTo(),','),
                "OUTPUT="+output
            };

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

            this.containerId = container.id();
            treatedBuildTracking.setContainerId(containerId);

            LOGGER.info("Start the container: "+containerName);
            docker.startContainer(container.id());

            ContainerExit exitStatus = docker.waitContainer(containerId);

            LOGGER.info("The container has finished with status code: "+exitStatus.statusCode());

            if (!skipDelete && exitStatus.statusCode() == 0) {
                LOGGER.info("Container will be removed.");
                docker.removeContainer(containerId);
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

    public void killDockerContainer(DockerClient docker, boolean remove) {
        if (this.containerId == null) {
            LOGGER.error("Error while trying to kill docker container: the container id is not available. Maybe the container is not started yet.");
        } else {
            LOGGER.info("Killing the docker container with id "+containerId+". Forced killing date: "+this.limitDateBeforeKilling);
            try {
                docker.killContainer(containerId);
                if (remove) {
                    docker.removeContainer(containerId);
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

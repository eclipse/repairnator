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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by urli on 14/03/2017.
 */
public class RunnablePipelineContainer implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(RunnablePipelineContainer.class);
    private String imageId;
    private int buildId;
    private String logDirectory;
    private RepairnatorConfig repairnatorConfig;
    private TreatedBuildTracking googleSpreadSheetTreatedBuildTracking;
    private boolean skipDelete;


    public RunnablePipelineContainer(String imageId, int buildId, String logDirectory, TreatedBuildTracking googleSpreadSheetTreatedBuildTracking, boolean skipDelete) {

        this.imageId = imageId;
        this.buildId = buildId;
        this.logDirectory = logDirectory;
        this.repairnatorConfig = RepairnatorConfig.getInstance();
        this.googleSpreadSheetTreatedBuildTracking = googleSpreadSheetTreatedBuildTracking;
        this.skipDelete = skipDelete;
    }

    @Override
    public void run() {
        String containerId = null;
        DockerClient docker = Launcher.docker;
        try {
            LOGGER.info("Start to build and run container for build id "+buildId);

            String containerName = "repairnator-pipeline_"+ Utils.formatFilenameDate(new Date())+"_"+this.buildId;
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
                "OUTPUT=/var/log"
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

            containerId = container.id();
            googleSpreadSheetTreatedBuildTracking.setContainerId(containerId);

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
            killDockerContainer(docker, containerId);
        } catch (DockerException e) {
            LOGGER.error("Error while creating or running the container for build id "+buildId, e);
            serialize("ERROR");
        }
        Launcher.submittedRunnablePipelineContainers.remove(this);
    }

    private void killDockerContainer(DockerClient docker, String containerId) {
        serialize("INTERRUPTED");
        try {
            docker.killContainer(containerId);
            docker.removeContainer(containerId);
        } catch (DockerException|InterruptedException e) {
            LOGGER.error("Error while killing docker container "+containerId, e);
        }

    }

    public void serialize(String msg) {
        googleSpreadSheetTreatedBuildTracking.setStatus(msg);
        googleSpreadSheetTreatedBuildTracking.serialize();
    }

}
